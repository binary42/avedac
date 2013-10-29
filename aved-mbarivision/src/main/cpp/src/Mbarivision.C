/*
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This is a program to automate detection and tracking of events in underwater 
 * video. This is based on modified version from Dirk Walther's 
 * work that originated at the 2002 Workshop  Neuromorphic Engineering 
 * in Telluride, CO, USA. 
 * 
 * This code requires the The iLab Neuromorphic Vision C++ Toolkit developed
 * by the University of Southern California (USC) and the iLab at USC. 
 * See http://iLab.usc.edu for information about this project. 
 *  
 * This work would not be possible without the generous support of the 
 * David and Lucile Packard Foundation
 */

#include "Channels/ChannelOpts.H"
#include "Component/GlobalOpts.H"
#include "Component/ModelManager.H"
#include "Component/JobServerConfigurator.H"
#include "Image/FilterOps.H"    // for lowPass5y()
#include "Image/Kernels.H"      // for twofiftyfives()
#include "Image/ColorOps.H"
#include "Image/MorphOps.H"
#include "Image/ShapeOps.H"   // for rescale()
#include "Raster/GenericFrame.H"
#include "Media/FrameRange.H"
#include "Media/FrameSeries.H"
#include "Media/SimFrameSeries.H"
#include "Media/MediaOpts.H"
#include "Neuro/SpatialMetrics.H"
#include "Neuro/StdBrain.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/VisualCortex.H"
#include "Simulation/SimEventQueueConfigurator.H"
#include "Util/sformat.H"
#include "Util/StringConversions.H"
#include "Util/Pause.H"
#include "Data/MbariMetaData.H"
#include "Data/MbariOpts.H"
#include "DetectionAndTracking/FOEestimator.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/Segmentation.H"
#include "DetectionAndTracking/ColorSpaceTypes.H"
#include "Image/MbariImage.H"
#include "Image/MbariImageCache.H"
#include "Image/BitObject.H"
#include "Media/MbariResultViewer.H"
#include "Utils/Version.H"
#include "Utils/MbariXMLParser.H"

#include <iostream>
#include <sstream>
#include <signal.h>

int main(const int argc, const char** argv) {

    // ######## Initialization of variables, reading of parameters etc.  
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    MbariMetaData metadata;
    Segmentation segmentation;
    //PauseWaiter p(3000000);
    //setPause(true);

    const int foaSizeRatio = 19;
    const int circleRadiusRatio = 40;

    //initialize a few things
    ModelManager manager("MBARI Automated Visual Event Detection Program");

    // turn down log messages until after initialzation
    MYLOGVERB = LOG_INFO;

    nub::soft_ref<SimEventQueueConfigurator>
            seqc(new SimEventQueueConfigurator(manager));
    manager.addSubComponent(seqc);

    nub::soft_ref<OutputFrameSeries> ofs(new OutputFrameSeries(manager));
    manager.addSubComponent(ofs);

    nub::soft_ref<InputFrameSeries> ifs(new InputFrameSeries(manager));
    manager.addSubComponent(ifs);

    nub::soft_ref<OutputFrameSeries> evtofs(new OutputFrameSeries(manager));
    manager.addSubComponent(evtofs);

    // Get the directory of this executable
    string exe(argv[0]);
    size_t found = exe.find_last_of("/\\");
    nub::soft_ref<MbariResultViewer> rv(new MbariResultViewer(manager, evtofs, ofs, exe.substr(0, found)));
    manager.addSubComponent(rv);

    nub::ref<DetectionParametersModelComponent> detectionParmsModel(new DetectionParametersModelComponent(manager));
    manager.addSubComponent(detectionParmsModel);

    nub::ref<StdBrain> brain(new StdBrain(manager));
    manager.addSubComponent(brain);

    // Request mbari specific option aliases
    REQUEST_OPTIONALIAS_MBARI(manager);

    // Request a bunch of toolkit option aliases
    REQUEST_OPTIONALIAS_NEURO(manager);

    // Initialize brain defaults
    manager.setOptionValString(&OPT_UseRandom, "true");
    manager.setOptionValString(&OPT_SVdisplayFOA, "true");
    manager.setOptionValString(&OPT_SVdisplayPatch, "false");
    manager.setOptionValString(&OPT_SVdisplayFOALinks, "false");
    manager.setOptionValString(&OPT_SVdisplayAdditive, "true");
    manager.setOptionValString(&OPT_SVdisplayTime, "false");
    manager.setOptionValString(&OPT_SVdisplayBoring, "false");
    
    // parse the command line
    if (manager.parseCommandLine(argc, argv, "", 0, -1) == false)
        LFATAL("Invalid command line argument. Aborting program now !");

    // set the range to be the same as the input frame range
    FrameRange fr = ifs->getModelParamVal<FrameRange > ("InputFrameRange");
    ofs->setModelParamVal(string("OutputFrameRange"), fr);
    evtofs->setModelParamVal(string("OutputFrameRange"), fr);

    // unset the rescaling in the event output frame series in case it is set
    evtofs->setModelParamVal(string("OutputFrameDims"), Dims(0, 0));

    // get image dimensions and set a few paremeters that depend on it
    detectionParmsModel->reset(&dp);

    // is this a a gray scale sequence ? if so disable computing the color channels
    // to save computation time. This assumes the color channel has no weight !
    if (dp.itsColorSpaceType == SAColorGray) {
        string search = "C";
        string source = manager.getOptionValString(&OPT_VisualCortexType);
        size_t pos = source.find(search);
        if (pos != string::npos) {
            string replace = source.erase(pos, 1);
            manager.setOptionValString(&OPT_VisualCortexType, replace);
        }
    }

    // get the dimensions of the raw input frames
    Dims dims = ifs->peekDims();
    float scaleW = 1.0f;
    float scaleH = 1.0f; 

    // get a reference to our original frame source including scaling 
    const nub::ref<FrameIstream> ref = ifs->getFrameSource(); 
    const Dims scaledDims = ref->peekDims();

    // if the user has selected to retain the original dimensions in the events
    // get the scaling factors
    if (dp.itsSaveOriginalFrameSpec) {
        scaleW = (float) scaledDims.w() / (float) dims.w();
        scaleH = (float) scaledDims.h() / (float) dims.h();
    }

    int foaRadius;
    const string foar = manager.getOptionValString(&OPT_FOAradius);
    convertFromString(foar, foaRadius);
 
    // calculate the foa size based on the image size if set to defaults
    // A zero foa radius indicates to set defaults from input image dims
    if (foaRadius == 0) {
        foaRadius = scaledDims.w() / foaSizeRatio;
        char str[256];
        sprintf(str, "%d", foaRadius);
        manager.setOptionValString(&OPT_FOAradius, str);
    }

    // initialize derived detection parameters
    const int circleRadius = dims.w() / circleRadiusRatio;

    // get reference to the SimEventQueue
    nub::soft_ref<SimEventQueue> seq = seqc->getQ();

    // start all the ModelComponents
    manager.start();

    int retval = 0;

    // set defaults for detection model parameters
    DetectionParametersSingleton::initialize(dp, dims, foaRadius);

    // get graph parameters
    vector<float> p = segmentation.getFloatParameters(dp.itsSegmentGraphParameters);
    const float sigma = segmentation.getSigma(p);
    const int k = segmentation.getK(p);
    const int min_size = segmentation.getMinSize(p);

    // initialize the visual event set
    VisualEventSet eventSet(dp, manager.getExtraArg(0));
    int countFrameDist = 1;

    // are we loading the event structure from a file?
    const bool loadedEvents = rv->isLoadEventsNameSet();
    if (loadedEvents) rv->loadVisualEventSet(eventSet);

    // initialize property vector and FOE estimator
    PropertyVectorSet pvs;
    FOEestimator foeEst(20, 0);

    // are we loading the set of property vectors from a file?
    const bool loadedProperties = rv->isLoadPropertiesNameSet();
    if (loadedProperties) rv->loadProperties(pvs);

    // initialize some more
    FrameRange frameRange = FrameRange::fromString(manager.getOptionValString(&OPT_InputFrameRange));
    ImageCacheAvg< PixRGB<byte> > avgCache(dp.itsSizeAvgCache);
    MbariImageCache< PixRGB<byte> > outCache(dp.itsSizeAvgCache);
    Image< PixRGB<byte> > img, img2runsaliency;
    MbariImage< PixRGB<byte> > mbariImg(manager.getOptionValString(&OPT_InputFrameSource).c_str());
    Image< PixRGB<byte> >savePreviousPicture(img.getDims(), ZEROS);
    float stddev = 0.f;

    // initialize the XML if requested to save event set to XML
    if (rv->isSaveXMLEventsNameSet()) {
        Image< PixRGB<byte> > tmpimg;
        MbariImage< PixRGB<byte> > mstart(manager.getOptionValString(&OPT_InputFrameSource).c_str());
        MbariImage< PixRGB<byte> > mend(manager.getOptionValString(&OPT_InputFrameSource).c_str());

        // get the starting and ending timecodes from the frames 
        nub::ref<FrameIstream> rep = ifs->getFrameSource();

        rep->setFrameNumber(frameRange.getFirst());
        tmpimg = rep->readRGB();
        mstart.updateData(tmpimg, frameRange.getFirst());

        rep->setFrameNumber(frameRange.getLast());
        tmpimg = rep->readRGB();
        mend.updateData(tmpimg, frameRange.getLast());

        // create the XML document with header information
        rv->createXMLDocument(Version::versionString(),
                frameRange,
                mstart.getMetaData().getTC(),
                mend.getMetaData().getTC(),
                dp);

        rep->setFrameNumber((frameRange.getFirst()));
    }

    string tc;
    // do we actually need to process the frames?
    if (rv->needFrames()) {
        while (avgCache.size() < dp.itsSizeAvgCache) {
            if (ifs->frame() >= frameRange.getLast()) {
                LERROR("Less input frames than necessary for sliding average - "
                        "using all the frames for caching.");
                break;
            }
            ifs->updateNext();
            img = ifs->readRGB();

            if (dp.itsMinStdDev > 0.f) {
                stddev = stdev(luminance(img));
                LINFO("Standard deviation in frame %d:  %f", ifs->frame(), stddev);

                // get the standard deviation in the input image
                // if there is little deviation do not add to the average cache
                if (stddev <= dp.itsMinStdDev && avgCache.size() > 0) {
                    LINFO("Standard deviation in frame %d too low. Is this frame all black ? Not including this image in the cache", ifs->frame());
                    avgCache.push_back(avgCache.mean());
                } else
                    avgCache.push_back(img);
            } else {
                avgCache.push_back(img);
            }

            // Get the MBARI metadata from the frame if it exists
            mbariImg.updateData(img, ifs->frame());
            tc = mbariImg.getMetaData().getTC();
            metadata = mbariImg.getMetaData();

            if (tc.length() > 0)
                LINFO("Caching frame %06d timecode: %s", ifs->frame(), tc.c_str());
            else
                LINFO("Caching frame %06d", ifs->frame());

            outCache.push_back(mbariImg);
        }
    } // end if needFrames 


    // ######## loop over frames ####################
    for (int curFrame = frameRange.getFirst(); curFrame <= frameRange.getLast(); ++curFrame) {
        rv->updateNext();
        if (rv->needFrames()) {
            // get image from cache or load and low-pass
            uint cacheFrameNum = curFrame - frameRange.getFirst();
            if (cacheFrameNum < avgCache.size()) {
                // we have cached this guy already
                LINFO("Processing frame %06d from cache.", curFrame);
                img = avgCache[cacheFrameNum];
                mbariImg = outCache[cacheFrameNum];
                metadata = mbariImg.getMetaData(); 

                if (dp.itsMinStdDev > 0.f) {
                    stddev = stdev(luminance(mbariImg));
                    LINFO("Standard deviation in frame %d:  %f", curFrame, stddev);
                }

                const list<BitObject> bitObjectFrameList = eventSet.getBitObjectsForFrame(curFrame - 1);
                if (!bitObjectFrameList.empty() && curFrame <= avgCache.size()) {
                    Image< PixRGB<byte> > imgToAddToTheBackground = getImageToAddToTheBackground(
                            mbariImg, avgCache.mean(),
                            savePreviousPicture,
                            bitObjectFrameList);
                    avgCache.push_back(imgToAddToTheBackground);
                    rv->output(imgToAddToTheBackground, curFrame - 1, "Background_input");
                }
                savePreviousPicture = mbariImg; // save the current frame for the next loop*/

            } else {
                // This means we are out of input
                if (ifs->frame() > frameRange.getLast()) {
                    LERROR("%d > %d Premature end of frame sequence - bailing out.", ifs->frame(), frameRange.getLast());
                    break;
                }

                const list<BitObject> bitObjectFrameList = eventSet.getColorBitObjectsForFrame(curFrame - 1);
                if (!bitObjectFrameList.empty()) {
                    Image< PixRGB<byte> > imgToAddToTheBackground = getImageToAddToTheBackground(
                            img, avgCache.mean(),
                            savePreviousPicture,
                            bitObjectFrameList);
                    avgCache.push_back(imgToAddToTheBackground);
                    rv->output(imgToAddToTheBackground, curFrame - 1, "Background_input");
                }

                savePreviousPicture = img; // save the current frame for the next loop*/

                ifs->updateNext();
                img = ifs->readRGB();

                if (dp.itsMinStdDev > 0.f) {
                    stddev = stdev(luminance(img));
                    LINFO("Standard deviation in frame %d:  %f", ifs->frame(), stddev);
                    // get the standard deviation in the input image

                    // if there is little deviation do not add to the average cache
                    if (stddev <= dp.itsMinStdDev && avgCache.size() > 0) {
                        LINFO("Standard deviation low in frame %d. Is this frame all black or just noise ? Not including this image in the cache", ifs->frame());
                        avgCache.push_back(avgCache.mean());
                    } else {
                        avgCache.push_back(img);
                    }
                } else {
                    avgCache.push_back(img);
                }

                // Get the MBARI metadata from the frame if it exists
                mbariImg.updateData(img, curFrame);
                tc = mbariImg.getMetaData().getTC();
                metadata = mbariImg.getMetaData();
                if (tc.length() > 0)
                    LINFO("Caching frame %06d timecode: %s", curFrame, tc.c_str());
                else
                    LINFO("Caching frame %06d", curFrame);
            }

            // Get the saliency input image
            if (dp.itsSaliencyInputType == SIDiffMean) {
                if (dp.itsSizeAvgCache > 1) {
                    img2runsaliency = avgCache.clampedDiffMean(mbariImg);
                } else
                    LFATAL("ERROR - must specify an imaging cache size "
                        "to use the DiffMean option. Try setting the"
                        "--mbari-cache-size option to something > 1");
            } else if (dp.itsSaliencyInputType == SIRaw) {
                img2runsaliency = mbariImg;
            } else {
                img2runsaliency = mbariImg;
            }

            rv->output(img2runsaliency, curFrame, "Saliency_input");

        } // end if needFrames

        rv->output(mbariImg, mbariImg.getFrameNum(), "Input");

	if (dp.itsSizeAvgCache > 1) rv->output(avgCache.mean(), mbariImg.getFrameNum(), "Background_mean");

        if (!loadedEvents) {

            // create a binary image for the segmentation
            Image<byte> bitImgShadow, bitImgHighlight, bitImgMasked;
            Image< byte > img2segment;
            Image<byte> bitImg(mbariImg.getDims(), ZEROS);
            Image< PixRGB<byte > > graphBitImg;
            std::list<WTAwinner> winlistGraph;

            // Create the binary image to segment
            if (dp.itsSegmentAlgorithmInputType == SAILuminance) {
                img2segment = luminance(mbariImg);
            } else if (dp.itsSegmentAlgorithmInputType == SAIDiffMean) {
                img2segment = maxRGB(avgCache.clampedDiffMean(mbariImg));
            } else {
                img2segment = maxRGB(avgCache.clampedDiffMean(mbariImg));
            }

            graphBitImg = segmentation.runGraph(sigma, k, min_size, winlistGraph, 1.0f, 1.0f, img2segment);
              
            list<BitObject> sobjs = getSalientObjects(graphBitImg, winlistGraph);

            if (dp.itsSegmentAlgorithmType == SAGraphCutOnly) {
                if (sobjs.size() > 0)
                    bitImg = showAllObjects(sobjs);
            }
            else if (dp.itsSegmentAlgorithmType == SAMeanAdaptiveThreshold) {
                bitImgShadow = segmentation.mean_thresh(img2segment, dp.itsMaxDist/2, dp.itsSegmentAdaptiveOffset);

                rv->output(bitImgShadow, mbariImg.getFrameNum(), "Segment_meanshadow");

                if (sobjs.size() > 0) {
                    bitImgHighlight = showAllObjects(sobjs);
                    rv->output(bitImgHighlight, mbariImg.getFrameNum(), "Segment_highlight");
                    bitImg = bitImgHighlight + bitImgShadow;
                } else {
                    bitImg = bitImgShadow;
                }
            }
            else if (dp.itsSegmentAlgorithmType == SAMedianAdaptiveThreshold) {
                bitImgShadow = segmentation.median_thresh(img2segment, dp.itsMaxDist/2, dp.itsSegmentAdaptiveOffset);

                rv->output(bitImgShadow, mbariImg.getFrameNum(), "Segment_medianshadow");

                if (sobjs.size() > 0) {
                    bitImgHighlight = showAllObjects(sobjs);
                    rv->output(bitImgHighlight, mbariImg.getFrameNum(), "Segment_highlight");
                    bitImg = bitImgHighlight + bitImgShadow;
                } else {
                    bitImg = bitImgShadow;
                }
            }
            else if (dp.itsSegmentAlgorithmType == SAMeanMinMaxAdaptiveThreshold) {
                bitImgShadow = segmentation.meanMaxMin_thresh(img2segment, dp.itsMaxDist/2, dp.itsSegmentAdaptiveOffset);

                rv->output(bitImgShadow, mbariImg.getFrameNum(), "Segment_minmaxshadow");

                if (sobjs.size() > 0) {
                    bitImgHighlight = showAllObjects(sobjs);
                    rv->output(bitImgHighlight, mbariImg.getFrameNum(), "Segment_highlight");
                    bitImg = bitImgHighlight + bitImgShadow;
                } else {
                    bitImg = bitImgShadow;
                }
            }

            rv->output(graphBitImg, mbariImg.getFrameNum(), "Graph_segment");
            
            // update the focus of expansion
            Vector2D curFOE = foeEst.updateFOE(bitImg);

            // cleanup image noise and display
            const Image<byte> se = twofiftyfives(dp.itsCleanupStructureElementSize);
            bitImg = erodeImg(dilateImg(bitImg, se), se);

            // mask special area in the frame we don't care
            bitImgMasked = maskArea(bitImg, &dp);

            rv->output(bitImgMasked, mbariImg.getFrameNum(), "Segment_output");

            // update the events with the segmented  images
            eventSet.updateEvents(bitImgMasked, curFOE, mbariImg.getFrameNum(), metadata);

            // is counter at 0?
            --countFrameDist;
            if (countFrameDist == 0) {
                countFrameDist = dp.itsSaliencyFrameDist;
                if (stddev >= dp.itsMinStdDev) {

                    LINFO("Getting salient regions for frame: %06d", mbariImg.getFrameNum());
                    
                    std::list<WTAwinner> winlist = getSalientWinners(img2runsaliency, 
			    brain, seq, dp.itsMaxEvolveTime, dp.itsMaxWTAPoints,
                            mbariImg.getFrameNum());

                    if (winlist.size() > 0) rv->output(showAllWinners(winlist, mbariImg, dp.itsMaxDist), mbariImg.getFrameNum(), "Winners");

                    list<BitObject> sobjs = getSalientObjects(bitImgMasked, winlist);

                    if (sobjs.size() > 0) rv->output(showAllObjects(sobjs), mbariImg.getFrameNum(), "Salient_Objects");

                    // initiate events with these objects
                    eventSet.initiateEvents(sobjs, mbariImg.getFrameNum(), metadata, scaleW, scaleH);
                }
            }

            // last frame? -> close everyone
            if (mbariImg.getFrameNum() == frameRange.getLast()) {
                eventSet.closeAll();
            }

            // prune invalid events
            eventSet.cleanUp(mbariImg.getFrameNum());

        } // end if (!loadedEvents)

        // this is a list of all the events that have a token in this frame
        list<VisualEvent *> eventFrameList;

        // this is a complete list of all those events that are ready to be written
        list<VisualEvent *> eventListToSave;

        if (!loadedEvents) {
            // get event frame list for this frame and those events that are ready to be saved
            // this is a list of all the events that have a token in this frame
            eventFrameList = eventSet.getEventsForFrame(mbariImg.getFrameNum());

            // this is a complete list of all those events that are ready to be written
            eventListToSave = eventSet.getEventsReadyToSave(mbariImg.getFrameNum());

            // write out eventSet?
            if (rv->isSaveEventsNameSet()) rv->saveVisualEvent(eventSet, eventFrameList);

            // write out summary ?
            if (rv->isSaveEventSummaryNameSet()) rv->saveVisualEventSummary(Version::versionString(), eventListToSave);

            // flag events that have been saved for delete
            list<VisualEvent *>::iterator i;
            for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
                (*i)->flagWriteComplete();

            // write out positions?
            if (rv->isSavePositionsNameSet()) rv->savePositions(eventFrameList);

            PropertyVectorSet pvsToSave = eventSet.getPropertyVectorSetToSave();

            // write out property vector set?
            if (rv->isSavePropertiesNameSet()) rv->saveProperties(pvsToSave);
        }

        // do this only when we actually load frames
        if (rv->needFrames()) {
            // need to obtain the property vector set?
            if (!loadedProperties) pvs = eventSet.getPropertyVectorSet();

            // get a list of events for this frame
            eventFrameList = eventSet.getEventsForFrame(mbariImg.getFrameNum());

            // write out eventSet to XML?
            if (rv->isSaveXMLEventsNameSet()) {
                rv->saveVisualEventSetToXML(eventFrameList,
                        mbariImg.getFrameNum(),
                        mbariImg.getMetaData().getTC(),
                        frameRange);
            }

            // display or write results  ?
            if (rv->isDisplayOutputSet() || rv->isSaveOutputSet()) {
                rv->outputResultFrame(mbariImg,
                        eventSet,
                        circleRadius);
            }

            // need to save any event clips?
            if (rv->isSaveAllEventClips()) {
                //save all events
                list<VisualEvent *>::iterator i;
                for (i = eventFrameList.begin(); i != eventFrameList.end(); ++i)
                    rv->saveSingleEventFrame(mbariImg, mbariImg.getFrameNum(), *i);
            } else {
                // need to save any particular event clips?
                uint csavenum = rv->numSaveEventClips();
                for (uint idx = 0; idx < csavenum; ++idx) {
                    uint evnum = rv->getSaveEventClipNum(idx);
                    if (!eventSet.doesEventExist(evnum)) continue;

                    VisualEvent *event = eventSet.getEventByNumber(evnum);
                    if (event->frameInRange(mbariImg.getFrameNum()))
                        rv->saveSingleEventFrame(mbariImg, mbariImg.getFrameNum(), event);
                }
            }
        }

        if (!loadedEvents) {
            //flag events that have been saved for delete otherwise takes too much memory
            list<VisualEvent *>::iterator i;
            for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
                (*i)->flagForDelete();
            while (!eventFrameList.empty()) eventFrameList.pop_front();
            while (!eventListToSave.empty()) eventListToSave.pop_front();
        }

        //if ( p.checkPause()) continue;
    } // end loop over all frames
    //######################################################
    LINFO("%s done!!!", PACKAGE);
    manager.stop();
    return retval;

} // end main


// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
