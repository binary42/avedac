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
#include "Data/MbariMetaData.H"
#include "Data/MbariOpts.H"
#include "DetectionAndTracking/FOEestimator.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/MbariImage.H"
#include "Image/MbariImageCache.H"
#include "Image/BitObject.H"
#include "Media/MbariResultViewer.H"
#include "Utils/Version.H"
#include "Utils/MbariXMLParser.H"

#include <iostream>
#include <sstream>
#include <signal.h>

using namespace std;

int main(const int argc, const char** argv) {

    // ######## Initialization of variables, reading of parameters etc.
    // a few constants
    DetectionParameters detectionParms;
    MbariMetaData metadata;
    Segmentation segmentation;

    const int maxSizeFactor = 200;
    const int maxDistRatio = 40;
    const int foaSizeRatio = 19;
    const int circleRadiusRatio = 40;
    const Image<byte> se = twofiftyfives(2);
    //initialize a few things
    ModelManager manager("MBARI Automated Visual Event Detection Program");

    // turn down log messages until after initialzation
    MYLOGVERB = LOG_NOTICE;

    nub::ref<JobServerConfigurator>
            jsc(new JobServerConfigurator(manager));
    manager.addSubComponent(jsc);

    nub::ref<SimEventQueueConfigurator>
            seqc(new SimEventQueueConfigurator(manager));
    manager.addSubComponent(seqc);

    nub::soft_ref<SimOutputFrameSeries> simofs(new SimOutputFrameSeries(manager));
    manager.addSubComponent(simofs);

    nub::soft_ref<InputFrameSeries> ifs(new InputFrameSeries(manager));
    manager.addSubComponent(ifs);

    nub::soft_ref<OutputFrameSeries> ofs(new OutputFrameSeries(manager));
    manager.addSubComponent(ofs);

    nub::soft_ref<OutputFrameSeries> evtofs(new OutputFrameSeries(manager));
    manager.addSubComponent(evtofs);

    // Get the binary directory of this executable
    string exe(argv[0]);
    size_t found = exe.find_last_of("/\\");
    nub::soft_ref<MbariResultViewer> rv(new MbariResultViewer(manager, evtofs, ofs, exe.substr(0,found)));
    manager.addSubComponent(rv);

    nub::soft_ref<DetectionParametersModelComponent> detectionParmsModel(new DetectionParametersModelComponent(manager));
    manager.addSubComponent(detectionParmsModel);

    nub::ref<StdBrain> brain(new StdBrain(manager));
    manager.addSubComponent(brain);

    // Request mbari specific option aliases
    REQUEST_OPTIONALIAS_MBARI(manager);

    // Request a bunch of toolkit option aliases
    REQUEST_OPTIONALIAS_NEURO(manager);

    // Initialize brain defaults
    manager.setOptionValString(&OPT_SVdisplayFOA, "true");
    manager.setOptionValString(&OPT_SVdisplayPatch, "false");
    manager.setOptionValString(&OPT_SVdisplayFOALinks, "false");
    manager.setOptionValString(&OPT_SVdisplayAdditive, "true");
    manager.setOptionValString(&OPT_SVdisplayTime, "false");
    manager.setOptionValString(&OPT_SVdisplayBoring, "false");

    // parse the command line
    if (manager.parseCommandLine(argc, argv, "",0,-1) == false)
	LFATAL("Invalid command line argument. Aborting program now !");
 
   // set the range to be the same as the input frame range
    FrameRange fr = ifs->getModelParamVal<FrameRange > ("InputFrameRange");
    ofs->setModelParamVal(string("OutputFrameRange"), fr);
    evtofs->setModelParamVal(string("OutputFrameRange"), fr);

    // unset the rescaling in the event output frame series in case it is set
    evtofs->setModelParamVal(string("OutputFrameDims"), Dims(0,0));

    // get image dimensions and set a few paremeters that depend on it
    detectionParmsModel->reset(&detectionParms);
    const Dims dims = ifs->peekDims();
    const int circleRadius = dims.w() / circleRadiusRatio;
    const int maxDist = dims.w() / maxDistRatio;
    const int foaSize = dims.w() / foaSizeRatio;
    char str[256];
    sprintf(str, "%d", foaSize);
    manager.setOptionValString(&OPT_FOAradius, str);
    const int minSize = foaSize;
    const int maxSize = minSize * maxSizeFactor;

    // initialize derived detection parameters
    detectionParms.itsMaxDist = maxDist; //pixels 

    // If these are set to the default, override them
    // with derived values,  otherwise these have been
    // set by the user so keep the user preferences
    if (detectionParms.itsMinEventArea == DEFAULT_MIN_EVENT_AREA)
        detectionParms.itsMinEventArea = minSize; //sq pixels
    if (detectionParms.itsMaxEventArea == DEFAULT_MAX_EVENT_AREA)
        detectionParms.itsMaxEventArea = maxSize; //sq pizels

    // calculate cost parameter from other parameters
    float maxDistFloat = (float) maxDist;
    float maxAreaDiff = pow((double) maxDistFloat, 2) / (double) 4.0;
    detectionParms.itsMaxCost = (float) maxDist / 2 * maxAreaDiff;
    if (detectionParms.itsTrackingMode == TMKalmanFilter)
        detectionParms.itsMaxCost = pow((double) maxDistFloat, 2) + pow((double) maxAreaDiff, 2);

    // get reference to the SimEventQueue
    nub::ref<SimEventQueue> seq = seqc->getQ();

    // start all the ModelComponents
    manager.start();

    int retval = 0;

    // set defaults for detection model parameters
    DetectionParametersSingleton::initialize(detectionParms);

    // initialize the visual event set
    VisualEventSet eventSet(detectionParms, manager.getExtraArg(0));
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
    ImageCacheAvg< PixRGB<byte> > avgCache(detectionParms.itsSizeAvgCache);
    ImageCacheAvg< byte > bwAvgCache(detectionParms.itsSizeAvgCache);
    MbariImageCache< PixRGB<byte> > outCache(detectionParms.itsSizeAvgCache);
    Image< PixRGB<byte> > img, img2runsaliency;
    Image< byte> img2segment;
    MbariImage< PixRGB<byte> > mbariImg(manager.getOptionValString(&OPT_InputFrameSource).c_str());

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
        // Creat the XML document with header information
        rv->createXMLDocument(Version::versionString(),
                frameRange,
                mstart.getMetaData().getTC(),
                mend.getMetaData().getTC(),
                detectionParms);
        
	rep->setFrameNumber((frameRange.getFirst()));
    }

    string tc;
    // do we actually need to process the frames?
    if (rv->needFrames()) {
	  while ( avgCache.size() < detectionParms.itsSizeAvgCache) {
            if (ifs->frame() >= frameRange.getLast() ) {
                LERROR("Less input frames than necessary for sliding average - "
                        "using all the frames for caching.");
		break;
            }
            ifs->updateNext(); 
            img = ifs->readRGB();

	   // get the standard deviation in the input image
           // if there is no deviation do not add to the average cache
	   // TODO: put a check here for all white/black pixels
    	   if (stdev(luminance(img)) == 0.f){ 
             LINFO("No standard deviation in frame %d. Is this frame all black ? Not including this image in the average cache", ifs->frame());
	     avgCache.push_back(avgCache.mean());
	   }
	   else
	     avgCache.push_back(img);
           
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
            uint cacheFrameNum = curFrame - frameRange.getFirst() ;
            if (cacheFrameNum < avgCache.size()) {
                // we have cached this guy already
                LINFO("Processing frame %06d from cache.", curFrame);
                img = avgCache[cacheFrameNum];
                mbariImg = outCache[cacheFrameNum];
            } else {
                // This means we are out of input
                if (ifs->frame() > frameRange.getLast()) {
                    LERROR("%d > %d Premature end of frame sequence - bailing out.", ifs->frame(), frameRange.getLast());
                    break;
                }
              
		ifs->updateNext();	
                img = ifs->readRGB();
                avgCache.push_back(img);
                mbariImg.updateData(img, curFrame);
                tc = mbariImg.getMetaData().getTC();
                metadata = mbariImg.getMetaData();
                if (tc.length() > 0)
                    LINFO("Caching frame %06d timecode: %s", curFrame, tc.c_str());
                else
                    LINFO("Caching frame %06d", curFrame);
             }

	    // Set the segment input type
            if (detectionParms.itsSegmentAlgorithmInputType == SAIMaxRGB) {
               	 img2segment = maxRGB(avgCache.absDiffMean(img));
	    }
            else if (detectionParms.itsSegmentAlgorithmInputType == SAILuminance)
                img2segment = luminance(img);
            else {	
                img2segment = maxRGB(avgCache.absDiffMean(img));
	    }
	    // Set the saliency input type
            if ( detectionParms.itsSaliencyInputType == SIDiffMean) {
            	if (detectionParms.itsSizeAvgCache > 1)
                  img2runsaliency = avgCache.clampedDiffMean(img);
		else
		  LFATAL("ERROR - must specify an imaging cache size "
                          "to use the DiffMean option. Try setting the"
                          "--mbari-cache-size option to something > 1");
	    }
            else if (detectionParms.itsSaliencyInputType == SIRaw) {
                 img2runsaliency = img;
            }
            else {
	         img2runsaliency = avgCache.clampedDiffMean(img);
	    }

        } // end if needFrames
        rv->output(img, curFrame, "Input");
        rv->output(img2runsaliency, curFrame, "Saliency_input");
        rv->output(img2segment, curFrame, "Segment_input");

        if (!loadedEvents) {

            bwAvgCache.push_back(img2segment);

            // create a binary image for the segmentation
            Image<byte> bitImg;
            Image< PixRGB<byte> > colorBitImg;
            const Image <PixRGB <byte> > background = avgCache.mean();

            //  Run selected segmentation algorithm
            if (detectionParms.itsSegmentAlgorithm == SABackgroundCanny)
                bitImg = segmentation.runBackgroundCanny(img2segment, segmentAlgorithmType(SABackgroundCanny));
            else if (detectionParms.itsSegmentAlgorithm == SAHomomorphicCanny)
                bitImg = segmentation.runHomomorphicCanny(img2segment, segmentAlgorithmType(SAHomomorphicCanny));
            else if (detectionParms.itsSegmentAlgorithm == SAAdaptiveThreshold)
                bitImg = segmentation.runAdaptiveThreshold(img2segment, segmentAlgorithmType(SAAdaptiveThreshold));
            else if (detectionParms.itsSegmentAlgorithm == SAExtractForegroundBW) {
                rv->output(background, curFrame, "Graphcut_mean_background");
                rv->output(img, curFrame, "Graphcut_segment_input");
               bitImg = segmentation.runGraphCut(img2segment, background, segmentAlgorithmType(SAExtractForegroundBW));
            }
            else if (detectionParms.itsSegmentAlgorithm == SABinaryAdaptive) {
            if (detectionParms.itsSizeAvgCache > 1)
                bitImg = segmentation.runBinaryAdaptive(bwAvgCache.clampedDiffMean(img2segment),
                        img2segment, detectionParms.itsTrackingMode);
	    else
                bitImg = segmentation.runBinaryAdaptive(img2segment,
                        img2segment, detectionParms.itsTrackingMode);

            } else
            if (detectionParms.itsSizeAvgCache > 1)
                bitImg = segmentation.runBinaryAdaptive(bwAvgCache.clampedDiffMean(img2segment), img2segment,
                    detectionParms.itsTrackingMode);
	    else
                bitImg = segmentation.runBinaryAdaptive(img2segment, img2segment,
                    detectionParms.itsTrackingMode);
        
            // If we are averaging frames, subtract from the average of the background,
            // otherwise, just use the input image
            if (detectionParms.itsSizeAvgCache > 1)
                colorBitImg = segmentation.test(avgCache.clampedDiffMean(img));
            else
                colorBitImg = segmentation.test(img);

            // update the focus of expansion
            Vector2D curFOE = foeEst.updateFOE(bitImg);

            // cleanup image noise and display
            bitImg = erodeImg(dilateImg(bitImg, se), se);

            // mask special area in the frame we don't care
            bitImg = maskArea(bitImg, &detectionParms);
            colorBitImg = maskArea(colorBitImg, &detectionParms);

            rv->output(colorBitImg, curFrame, "Segment_color_output");
            rv->output(bitImg, curFrame, "Segment_output");

            // update the events with the segmented images
            eventSet.updateEvents(bitImg, curFOE, mbariImg.getFrameNum(), metadata);

            // is counter at 0?
            --countFrameDist;
            if (countFrameDist == 0) {
                countFrameDist = detectionParms.itsSaliencyFrameDist;

                LINFO("Getting salient regions for frame: %06d", mbariImg.getFrameNum());

                const float maxEvolveTime = detectionParms.itsMaxEvolveTime;
                const uint maxNumSalSpots = detectionParms.itsMaxWTAPoints;
                list<WTAwinner> winlist = getSalientWinners(simofs,
                        img2runsaliency, brain, seq, maxEvolveTime, maxNumSalSpots,
                        mbariImg.getFrameNum());

                list<BitObject> sobjs = getSalientObjects(bitImg, colorBitImg, winlist);

                if (sobjs.size() > 0) rv->output(showAllObjects(sobjs), curFrame, "Salient_Objects");

                // initiate events with these objects
                eventSet.initiateEvents(sobjs, mbariImg.getFrameNum(), metadata);
            }

            // last frame? -> close everyone
            if (mbariImg.getFrameNum() == frameRange.getLast()) {
			LDEBUG("closing events");
			eventSet.closeAll();
            }

            // clean up small events, and those needing deletion.
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
            if (rv->isSaveXMLEventsNameSet())
                rv->saveVisualEventSetToXML(eventFrameList,
                    mbariImg.getFrameNum(),
                    mbariImg.getMetaData().getTC(),
                    frameRange);


            rv->outputResultFrame(mbariImg,
                    eventSet,
                    circleRadius);

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
