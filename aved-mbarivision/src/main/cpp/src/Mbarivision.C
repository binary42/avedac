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

#include "Image/OpenCVUtil.H"
#include "Channels/ChannelOpts.H"
#include "Component/GlobalOpts.H"
#include "Component/ModelManager.H"
#include "Component/JobServerConfigurator.H"
#include "Features/HistogramOfGradients.H"
#include "Image/FilterOps.H"    // for lowPass3y()
#include "Image/Kernels.H"      // for twofiftyfives()
#include "Image/ColorOps.H"
#include "Image/fancynorm.H"
#include "Image/MorphOps.H"
#include "Image/ShapeOps.H"   // for rescale()
#include "Learn/Bayes.H"
#include "Raster/GenericFrame.H"
#include "Raster/PngWriter.H"
#include "Media/FrameRange.H"
#include "Media/FrameSeries.H"
#include "Media/SimFrameSeries.H"
#include "Media/MediaOpts.H"
#include "Neuro/SpatialMetrics.H"
#include "Neuro/StdBrain.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/Retina.H"
#include "Neuro/VisualCortex.H"
#include "SIFT/Histogram.H"
#include "Simulation/SimEventQueueConfigurator.H"
#include "Util/sformat.H"
#include "Util/StringConversions.H"
#include "Util/Pause.H"
#include "Data/Logger.H"
#include "Data/MbariMetaData.H"
#include "Data/MbariOpts.H"
#include "DetectionAndTracking/FOEestimator.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/Segmentation.H"
#include "DetectionAndTracking/ColorSpaceTypes.H"
#include "DetectionAndTracking/ObjectDetection.H"
#include "DetectionAndTracking/Preprocess.H"
#include "Image/MbariImage.H"
#include "Image/MbariImageCache.H"
#include "Image/BitObject.H"
#include "Image/DrawOps.H"
#include "Image/IO.H"
#include "Media/MbariResultViewer.H"
#include "Motion/MotionEnergy.H"
#include "Motion/MotionOps.H"
#include "Motion/OpticalFlow.H"
#include "Utils/Version.H"
#include "Utils/MbariXMLParser.H"

#include <iostream>
#include <sstream>
#include <signal.h>

//#define DEBUG

using namespace MbariVisualEvent;

int main(const int argc, const char** argv) {

    // ######## Initialization of variables, reading of parameters etc.
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    MbariMetaData metadata;
    Segmentation segmentation;
    #ifdef DEBUG
    PauseWaiter pause;
    setPause(true);
    #endif
    const int foaSizeRatio = 19;

    //initialize a few things
    ModelManager manager("MBARI Automated Visual Event Detection Program");

    // turn down log messages until after initialization
    MYLOGVERB = LOG_INFO;

    nub::soft_ref<SimEventQueueConfigurator>
            seqc(new SimEventQueueConfigurator(manager));
    manager.addSubComponent(seqc);

    nub::soft_ref<OutputFrameSeries> ofs(new OutputFrameSeries(manager));
    manager.addSubComponent(ofs);

    nub::soft_ref<InputFrameSeries> ifs(new InputFrameSeries(manager));
    manager.addSubComponent(ifs);

    nub::soft_ref<ObjectDetection> objdet(new ObjectDetection(manager));
    manager.addSubComponent(objdet);

    nub::soft_ref<Preprocess> preprocess(new Preprocess(manager));
    manager.addSubComponent(preprocess);

    // Get the directory of this executable
    string exe(argv[0]);
    size_t found = exe.find_last_of("/\\");
    nub::soft_ref<Logger> logger(new Logger(manager, ifs, ofs, exe.substr(0, found)));
    manager.addSubComponent(logger);

    nub::soft_ref<MbariResultViewer> rv(new MbariResultViewer(manager, logger));
    manager.addSubComponent(rv);

    nub::ref<DetectionParametersModelComponent> parms(new DetectionParametersModelComponent(manager));
    manager.addSubComponent(parms);

    nub::ref<StdBrain> brain(new StdBrain(manager));
    manager.addSubComponent(brain);

    // Request MBARI specific option aliases
    REQUEST_OPTIONALIAS_MBARI(manager);

    // Request a bunch of toolkit option aliases
    REQUEST_OPTIONALIAS_NEURO(manager);

    // Initialize brain defaults
    manager.setOptionValString(&OPT_UseRandom, "true");
  /*  manager.setOptionValString(&OPT_SVdisplayFOA, "true");
    manager.setOptionValString(&OPT_SVdisplayPatch, "false");
    manager.setOptionValString(&OPT_SVdisplayFOALinks, "false");
    manager.setOptionValString(&OPT_SVdisplayAdditive, "true");
    manager.setOptionValString(&OPT_SVdisplayTime, "false");
    manager.setOptionValString(&OPT_SVdisplayBoring, "false");*/

    // parse the command line
    if (manager.parseCommandLine(argc, argv, "", 0, -1) == false)
        LFATAL("Invalid command line argument. Aborting program now !");

    // set the range to be the same as the input frame range
    FrameRange fr = ifs->getModelParamVal< FrameRange > ("InputFrameRange");
    ofs->setModelParamVal(string("OutputFrameRange"), fr);

    // get image dimensions and set a few parameters that depend on it
    parms->reset(&dp);

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

  // get the dimensions of the potentially scaled input frames
    Dims scaledDims = ifs->peekDims();

    // if the user has selected to retain the original dimensions in the events disable scaling in the frame series
    // and use the scaling factors directly
    if (dp.itsSaveOriginalFrameSpec) {
        ofs->setModelParamVal(string("OutputFrameDims"), Dims(0, 0));
        ifs->setModelParamVal(string("InputFrameDims"), Dims(0, 0));
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

    // get reference to the SimEventQueue
    nub::soft_ref<SimEventQueue> seq = seqc->getQ();

    // start all the ModelComponents
    manager.start();

    // set defaults for detection model parameters
    DetectionParametersSingleton::initialize(dp, scaledDims, foaRadius);

    // initialize the visual event set
    VisualEventSet eventSet(dp, manager.getExtraArg(0));

    // initialize masks
    Image<byte> mask(scaledDims, ZEROS);
    mask = highThresh(mask, byte(0), byte(255));

    Image<byte> staticClipMask(scaledDims, ZEROS);
    mask = highThresh(mask, byte(0), byte(255));
    staticClipMask = maskArea(mask, &dp);

    // initialize the default mask
    mask = staticClipMask;

    // initialize the preprocess
    preprocess->init(ifs, scaledDims);
    ifs->reset1(); //reset to state after construction since the preprocessing caches input frames

    // main loop:
    printf("\nMAIN_LOOP\n\n");

    int numSpots = 0;
    uint frameNum = 0;
    std::string outStr(manager.getOptionValString(&OPT_InputFrameSource).c_str());
    MbariImage< PixRGB<byte> > input(manager.getOptionValString(&OPT_InputFrameSource).c_str());
    MbariImage< PixRGB<byte> > prevInput(manager.getOptionValString(&OPT_InputFrameSource).c_str());
    MbariImage< PixRGB <byte> > output(manager.getOptionValString(&OPT_InputFrameSource).c_str());
    Image<byte>  foaIn;
    Image<byte> binSegmentOut(input.getDims(), ZEROS);
    Image< PixRGB<byte> > segmentIn(input.getDims(), ZEROS);
    Image< PixRGB<byte> > inputRaw, inputScaled;
    Image<byte> mmap, prevmmap;

    // count between frames to run saliency; note two are required to compute motion and flick channel, otherwise can
    // compute every frame in the case of still frames, or fast moving video with lots of interesting content
    uint countFrameDist = dp.itsSaliencyFrameDist > 1 ? 2 : 1;
    bool hasCovert; // flag to monitor whether visual cortex had any output

    // initialize property vector and FOE estimator
    PropertyVectorSet pvs;
    FOEestimator foeEst(20, 0);
    Vector2D curFOE;
    SimTime prevTime = seq->now();

    MotionEnergyPyrBuilder motion(Gaussian5, 1.0F);

    // bayesian network with 324 features
    Bayes bn(324, 0);
    bn.load("/home/avedac/Desktop/avedac/examples/dws20140919_sledI_00006/bayes.net");

    // create hog
    bool normalizeHistogram = true;
    bool fixedHistogram = true; // if false, cell size fixed
    Dims cellSize = Dims(3,3); // if fixedHist is true, this is hist size, if false, this is cell size
    //8x8 = 1296 features
    //3x3 =  36 features
    HistogramOfGradients hog(normalizeHistogram,cellSize,fixedHistogram);

    std::string featureFileName = "predictions.txt";
    std::ofstream featureFile;
    featureFile.open(featureFileName.c_str(),std::ios::out);

    while(1)
    {

     // read new image in?
     const FrameState is = ifs->updateNext();

     if (is == FRAME_COMPLETE) break; // done
     if (is == FRAME_NEXT || is == FRAME_FINAL) // new frame
     {
        LINFO("Reading new frame");
        numSpots = 0;

        //  cache and enhance image
        inputRaw = ifs->readRGB();
        inputScaled = rescale(inputRaw, scaledDims);

        // get updated input image erasing previous bit objects
        const list<BitObject> bitObjectFrameList = eventSet.getBitObjectsForFrame(frameNum - 1);

        // update the background cache
        if (!bitObjectFrameList.empty())
            input = preprocess->update(inputScaled, prevInput, ifs->frame(), bitObjectFrameList);
        else
            input = preprocess->update(inputScaled, ifs->frame());

        frameNum = input.getFrameNum();

        rv->output(ofs, input, frameNum, "Input");

        // choose image to segment; these produce different results and vary depending on midwater/benthic/etc.
        if (dp.itsSegmentAlgorithmInputType == SAILuminance) {
            segmentIn = input;
        } else if (dp.itsSegmentAlgorithmInputType == SAIDiffMean) {
            segmentIn = preprocess->clampedDiffMean(input);
        } else {
            segmentIn = preprocess->clampedDiffMean(input);
        }

        segmentIn = maskArea(segmentIn, mask);

        rv->output(ofs, segmentIn, frameNum, "SegmentIn");

        /*Segmentation s;
        Dims d = segmentIn.getDims();
        Rectangle r = Rectangle::tlbrI(0,0,d.h()-1,d.w()-1);
        Image<PixRGB<byte>> t = s.runGraph(segmentIn, r, 1.0);
        rv->output(ofs, t, frameNum, "Segment1.0");
        t = s.runGraph(segmentIn, r, 0.5);
        rv->output(ofs, t, frameNum, "Segment.5");*/

       // rutz::shared_ptr<OpticalFlow> flow =
      //   getLucasKanadeOpticFlow(prevLum, lum);
      // Image<PixRGB<byte> > opticFlow = drawOpticFlow(prevImage, flow);
      // Image< PixRGB<byte> > disp(4*width, height, ZEROS);
      // inplacePaste(disp, prevImage, Point2D<int>(0    ,0));
      // inplacePaste(disp, currImage, Point2D<int>(width,0));

        Image<byte> hh(input.getDims(), ZEROS);
        hh = highThresh(hh, byte(0), byte(255));
        mmap = motion.updateMotion(luminance(inputScaled), hh);
        rv->output(ofs, mmap, frameNum, "MMAP");

        if (prevInput.initialized()) {

            // compute the optic flow
            /*rutz::shared_ptr<MbariOpticalFlow> flow =  getOpticFlow
            (Image<byte>(luminance(mmap2)),
            Image<byte>(luminance(mmap)));

            Image<PixRGB<byte> > opticFlow = drawOpticFlow(prevInput, flow);
            rv->output(ofs, opticFlow, frameNum, "opticflow");
            rv->output(ofs, flow->getFlowStrengthField(), frameNum, "OFStrength");
            rv->output(ofs, flow->getVectorLengthField(), frameNum, "OFLength");

            std::vector<rutz::shared_ptr<MbariFlowVector> > vectors = flow->getFlowVectors();
            Image< float > ix(prevInput.getDims(), ZEROS);
            Image< float > iy(prevInput.getDims(), ZEROS);
            
              // we are going to assume we have a sparse flow field
              // and random access on the field
              for(uint v = 0; v < vectors.size(); v++)
                {
                  Point2D<float> pt = vectors[v]->p1;
                  uint i = pt.i;
                  uint j = pt.j;
                  float xmag  = 200.0f * vectors[v]->xmag;
                  float ymag  = 200.0f * vectors[v]->ymag;
                  //ix.setVal(i,j, xmag );
                  //iy.setVal(i,j, ymag );
                  if(xmag > 0.0) ix.setVal(i,j, xmag );
                  if(ymag > 0.0) iy.setVal(i,j, ymag );
             }

            int numBins = 8;
            const int lineSize = 20;

            Image<float> mag, ori;
            gradientSobel(flow->getFlowStrengthField(), mag, ori, 3);
            rv->output(ofs, static_cast< Image<byte> >(mag), frameNum, "OFXYm");
            rv->output(ofs, static_cast< Image<byte> >(ori), frameNum, "OFXYo");
          //  rv->output(ofs, static_cast< Image<byte> >(ix), frameNum, "ix");
          //  rv->output(ofs, static_cast< Image<byte> >(ix), frameNum, "ix");
          //  rv->output(ofs, static_cast< Image<byte> >(mag), frameNum, "XGM");

          //  ImageSet<float> imgFeatures = getFeatures(ix, numBins);
          //  Image<PixRGB<byte> > hogImage = getHistogramImage(imgFeatures, lineSize);
           // rv->output(ofs, hogImage, frameNum, "ixHOG");

         //   gradientSobel(iy, mag, ori, 3);
         //   rv->output(ofs, static_cast< Image<byte> >(iy), frameNum, "iy");
         //   rv->output(ofs, static_cast< Image<byte> >(mag), frameNum, "YGM");

         //   imgFeatures = getFeatures(iy, numBins);
          //  hogImage = getHistogramImage(imgFeatures, lineSize);
           // rv->output(ofs, hogImage, frameNum, "iyHOG");

            Image<float> yFilter(1, 3, ZEROS);
            Image<float> xFilter(3, 1, ZEROS);
            yFilter.setVal(0, 0, 1.F);
            yFilter.setVal(0, 2, 1.F);
            xFilter.setVal(0, 0, 1.F);
            xFilter.setVal(2, 0, 1.F);

            Image<float> yiy = sepFilter(iy, Image<float>(), yFilter, CONV_BOUNDARY_CLEAN);
            Image<float> xix = sepFilter(ix, xFilter, Image<float>(), CONV_BOUNDARY_CLEAN);

            rv->output(ofs, static_cast< Image<byte> >(yiy), frameNum, "YYder");
            rv->output(ofs, static_cast< Image<byte> >(xix), frameNum, "XXder");

            Image<float> yix = sepFilter(iy, xFilter, Image<float>(), CONV_BOUNDARY_CLEAN);
            Image<float> xiy = sepFilter(ix, Image<float>(), yFilter, CONV_BOUNDARY_CLEAN);

            rv->output(ofs, static_cast< Image<byte> >(yix), frameNum, "YXder");
            rv->output(ofs, static_cast< Image<byte> >(xiy), frameNum, "XYder");

            ImageSet<float> imgFeatures = getFeatures(mmap, 4);
            Image<PixRGB<byte> > hogImage = getHistogramImage(imgFeatures, lineSize);
            rv->output(ofs, hogImage, frameNum, "HOG1a");

            imgFeatures = getFeatures(mmap, 8);
            hogImage = getHistogramImage(imgFeatures, lineSize);
            rv->output(ofs, hogImage, frameNum, "HOG1b");

            imgFeatures = getFeatures(segmentIn, numBins);
            hogImage = getHistogramImage(imgFeatures, lineSize);
            rv->output(ofs, hogImage, frameNum, "HOG2");

            imgFeatures = getFeatures(input, numBins);
            hogImage = getHistogramImage(imgFeatures, lineSize);
            rv->output(ofs, hogImage, frameNum, "HOG3");*/

            /*
            int numBins = 12;
            LINFO("Computing HOG");
            const int lineSize = 2;
            ImageSet<float> hist1 = getFeatures(xix, numBins);
            ImageSet<float> hist2 = getFeatures(yix, numBins);
            Image<PixRGB<byte> > hogImage1 = getHistogramImage(hist1, lineSize);
            Image<PixRGB<byte> > hogImage2 = getHistogramImage(hist2, lineSize);
            Image<PixRGB<byte>> outHist =  hogImage1 - hogImage2;
            rv->output(ofs, outHist, frameNum, "IMHdiffx");

            hist1 = getFeatures(xiy, numBins);
            hist2 = getFeatures(yiy, numBins);
            hogImage1 = getHistogramImage(hist1, lineSize);
            hogImage2 = getHistogramImage(hist2, lineSize);
            outHist =  hogImage1 - hogImage2;
            rv->output(ofs, outHist, frameNum, "IMHdiffy");

            ImageSet<float> imgFeatures = getFeatures(input, numBins);
            hogImage2 = getHistogramImage(imgFeatures, lineSize);
            rv->output(ofs, hogImage2, frameNum, "HOGinput");*/

            //hogImage2 = getHistogramImage(filterFeatures, lineSize);
            //rv->output(ofs, hogImage2, frameNum - 1, "HOGevt", (*event)->getEventNum());

            //ImageSet<float> filterFeatures = getFeatures(evtImg, 1);
            //std::vector<float> ret = HistogramOfGradients::createHistogramFromGradient(gradmag,gradang);
            //std::vector<float> tmp = calculateJunctionHistogram(gradmag,gradang);
            //ret.insert(ret.end(),tmp.begin(),tmp.end());

            // this is a list of all the events that have a token in this frame
            std::list<MbariVisualEvent::VisualEvent *> eventFrameList;
            eventFrameList = eventSet.getEventsForFrame(frameNum - 1);

            // dump information about the bayes classifier
            //for(uint i=0; i<bn.getNumFeatures(); i++)
            //  LINFO("Feature %i: mean %f, stddevSq %f", i, bn.getMean(0, i), bn.getStdevSq(0, i));

            // for each bit object, extract features and save the output
            std::list<MbariVisualEvent::VisualEvent *>::iterator event;
            for (event = eventFrameList.begin(); event != eventFrameList.end(); ++event) {

                LINFO("==============================================searching for event %d", (*event)->getEventNum());

                Rectangle bbox = (*event)->getToken(frameNum-1).bitObject.getBoundingBox();
                Image< PixRGB<byte> > in =  preprocess->clampedDiffMean(prevInput);
                std::vector<double> features = getFeatures2(prevmmap, in, hog, scaledDims, bbox);

                // try to classify using histogram features
                //double prob = 0.;
                // int cls = bn.classify(features, &prob);

                // create the file stem and write out the features
                string evnum(sformat("evt%04d_%06d.dat", (*event)->getEventNum(), frameNum-1));
                std::ofstream eofs(evnum);
                eofs.precision(12);
                std::vector<double>::iterator eitr = features.begin(), stop = features.end();
                while(eitr != stop)   eofs << *eitr++ << " ";
                eofs.close();

                // if probability small, no matches found, so add a new class by event number
                //if (prob < 0.1) {
                //if ((*event)->getEventNum() <  maxClasses) {
                //    bn.learn(features,  (*event)->getEventNum());
                // }

                 // calculate the bounding box for sliding window based on the prediction
                 const Point2D<int> pred = (*event)->predictedLocation();

                 Dims dims = input.getDims();

                 // is the prediction too far outside the image?
                 int gone = dp.itsMaxDist;
                 if ((pred.i < -gone) || (pred.i >= (dims.w() + gone)) ||
                     (pred.j < -gone) || (pred.j >= (dims.h() + gone)))
                   {
                     break;
                   }

                 // adjust prediction if negative
                 const Point2D<int> center =  Point2D<int>(std::max(pred.i,0), std::max(pred.j,0));

                 // get the region used for searching for a match based on the maximum dimension
                 Dims maxDims = (*event)->getMaxObjectDims();
                 Dims searchDims = Dims((float)maxDims.w()*4.0,(float)maxDims.h()*4.0);
                 Rectangle region = Rectangle::centerDims(center, searchDims);
                 region = region.getOverlap(Rectangle(Point2D<int>(0, 0), inputRaw.getDims() - 1));
                 Dims windowDims = (*event)->getToken(frameNum - 1).bitObject.getObjectDims();

                 // run overlapping sliding box over a region centered at the predicted location
                 Dims stepDims(windowDims.w()/4.F, windowDims.h()/4.F);
            }
        }

        // update the focus of expansion - is this still needed ?
        foaIn = luminance(input);
        const byte threshold = mean(foaIn);
        curFOE = foeEst.updateFOE(makeBinary(foaIn, threshold));

        // update the open events
        eventSet.updateEvents(rv, mask, frameNum, input, prevInput, segmentIn, curFOE, metadata);

        // is counter within 1 of reset? queue two successive images in the brain for motion and flicker computation
        --countFrameDist;
        if (countFrameDist <= 1 ) {

            Image< PixRGB<byte> > brainInput;

            // Get image to input into the brain
            if (dp.itsSaliencyInputType == SIDiffMean) {
                if (dp.itsSizeAvgCache > 1)
                    brainInput = preprocess->absDiffMean(input);
                else
                    LFATAL("ERROR - must specify an imaging cache size "
                        "to use the DiffMean option. Try setting the"
                        "--mbari-cache-size option to something > 1");
            }
            else if (dp.itsSaliencyInputType == SIRaw) {
                brainInput = rescale(inputRaw, Dims(960,540));
            }
            else
                brainInput = input;

            rv->display(brainInput, frameNum, "BrainInput");;

            // post new input frame for processing
            rutz::shared_ptr<SimEventInputFrame> e(new SimEventInputFrame(brain.get(), GenericFrame(brainInput), 0));
            seq->post(e);

        }

    }

    // check for map output and mask if needed on frame before saliency run
    // the reason mask here and not in the pyramid is because the blur around the inside of the clip mask in the model
    // can mask out interesting objects, particularly for large masks around the edge
    SeC<SimEventVisualCortexOutput> s = seq->check<SimEventVisualCortexOutput>(brain.get());
    if ( s && (is == FRAME_NEXT || is == FRAME_FINAL) && countFrameDist == 0  ) {

        LINFO("Updating visual cortex output for frame %d", frameNum);

        // get saliency map and dimensions
        Image<float> sm = s->vco();
        Dims dimsm = sm.getDims();

        // update the laser mask
        if (dp.itsMaskLasers) {
            LINFO("Masking lasers in L*a*b color space");
            Image<PixRGB<float> > in = input;
            Image<byte>::iterator mitr = mask.beginw();
            Image<PixRGB<float>>::const_iterator ritr = in.beginw(), stop = in.end();
            float thresholda = 50.F, thresholdl = 50.F;
            // mask out any significant red in the L*a*b color space where strong red has positive a values
            while(ritr != stop) {
                const PixLab<float> pix = PixLab<float>(*ritr++);
                float l = pix.p[0]/3.0F; // 1/3 weight
                float a = pix.p[1]/3.0F; // 1/3 weight
                *mitr++  = (a > thresholda && l > thresholdl) ? 0 : *mitr;
            }
        }

        // mask is inverted so morphological operations are in reverse; here we are enlarging the mask to cover
        Image<byte> se = twofiftyfives(dp.itsCleanupStructureElementSize);
        mask = erodeImg(mask, se);
        rv->output(ofs, mask, frameNum, "Mask");

        // rescale the mask if needed
        Image<byte> maskRescaled = rescale(mask, dimsm);

        // mask out equipment, etc.
        Image<float>::iterator smitr = sm.beginw();
        Image<byte>::const_iterator mitr = maskRescaled.beginw(), stop = maskRescaled.end();

        // set voltage to 0 where mask is 0
        while(mitr != stop) {
           *smitr  = ( (*mitr) == 0 ) ? 0.F : *smitr;
           mitr++; smitr++;
        }

        // post as new output so other simulation modules can iterate on this
        rutz::shared_ptr<SimEventVisualCortexOutput> newsm(new SimEventVisualCortexOutput(brain.get(), sm));
        seq->post(newsm);
    }

    hasCovert = false;

    // reached distance between computing saliency in frames ?
    if (countFrameDist == 0) {
        countFrameDist = dp.itsSaliencyFrameDist;

        // initialize the max time to simulate
        const SimTime simMaxEvolveTime = seq->now() + SimTime::MSECS(dp.itsMaxEvolveTime);

        std::list<Winner> winlist;
        std::list<BitObject> objs;
        float scaleH = 1.0f, scaleW = 1.0F;

        // search for new winners until reached max time, max spots or boring WTA point
        while (seq->now().msecs() < simMaxEvolveTime.msecs()) {

            // evolve the brain and other simulation modules
            seq->evolve();

            // found a new winner ?
            if (SeC<SimEventWTAwinner> e = seq->check<SimEventWTAwinner>(brain.get())) {
                LINFO("##### time now:%f msecs max evolve time:%f msecs frame: %d #####", \
                        seq->now().msecs(), simMaxEvolveTime.msecs(), frameNum);
                hasCovert = true;
                numSpots++;
                WTAwinner win = e->winner();
                LINFO("##### winner #%d found at [%d; %d] with %f voltage frame: %d#####",
                        numSpots, win.p.i, win.p.j, win.sv, frameNum);

                // winner not boring, or if keeping boring winners
                // grab Focus Of Attention (FOA) mask shape to later guide winner selection
                if (SeC<SimEventShapeEstimatorOutput> se = seq->check<SimEventShapeEstimatorOutput>(brain.get())) {
                    Image<byte> foamask = Image<byte>(se->smoothMask()*255);

                    // rescale if needed
                    if (scaledDims != foamask.getDims()) {
                        scaleW = (float) scaledDims.w()/(float) foamask.getDims().w();
                        scaleH = (float) scaledDims.h()/(float) foamask.getDims().h();
                        foamask = rescale(foamask, scaledDims);
                        win.p.i = (int) ( (float) win.p.i*scaleW );
                        win.p.j = (int) ( (float) win.p.j*scaleH );
                    }

                    // create bit object out of FOA mask
                    BitObject bo;
                    bo.reset(makeBinary(foamask,byte(0),byte(0),byte(1)));
                    bo.setSMV(win.sv);

                    // if have valid bit object out of the FOA mask, run object detection
                    if (bo.isValid() && bo.getArea() >= dp.itsMinEventArea) {
                        Winner w(win, bo, frameNum);
                        std::list<BitObject> bos = objdet->run(rv, input, w, segmentIn);
                        objs.splice(objs.end(), bos);
                        winlist.push_back(w);
                    }
                }

                if (win.boring) {
                    LINFO("##### boring event detected #####");
                    break;
                }

                if (numSpots >= dp.itsMaxWTAPoints) {
                    LINFO("##### found maximum number of salient spots #####");
                    break;
                }

                if (seq->now().msecs() >= simMaxEvolveTime.msecs()) {
                    LINFO("##### time limit reached time now:%f msecs max evolve time:%f msecs frame: %d #####", \
                            seq->now().msecs(), simMaxEvolveTime.msecs(), frameNum);
                    break;
                }

        } // check for winner

        }// end brain while iteration loop

        // create new events with this
        eventSet.initiateEvents(objs, frameNum, metadata, segmentIn, curFOE);

        rv->output(ofs, showAllWinners(winlist, input, dp.itsMaxDist), frameNum, "Winners");
        winlist.clear();
        objs.clear();

        prevTime = seq->now(); // time before current step
    }

    const FrameState os = ofs->updateNext();

    if (os == FRAME_NEXT || os == FRAME_FINAL) {
        // create MBARI image with metadata from input and original input frame
        output.updateData(inputRaw, input.getMetaData(), ofs->frame());

        // prune invalid events
        eventSet.cleanUp(ofs->frame());

        // write out/display anything that's ready
        logger->run(rv, output, eventSet, scaledDims);

        // save anything requested from brain model
        if (hasCovert)
            brain->save(SimModuleSaveInfo(ofs, *seq));

        // save the input image
        prevInput = input;
        prevmmap = mmap;

        // reset the brain, but only when distance between running saliency is more than every frame
        if (countFrameDist == dp.itsSaliencyFrameDist && dp.itsSaliencyFrameDist > 1) {
            brain->reset(MC_RECURSE);
            seq->resetTime(prevTime);
        }
    }

    if (os == FRAME_FINAL) {
         // last frame? -> close everyone
        eventSet.closeAll();
    break;
    }

    #ifdef DEBUG
    if ( pause.checkPause()) Raster::waitForKey();// || ifs->shouldWait() || ofs->shouldWait()) Raster::waitForKey();
    continue;
    #endif
    } // end while
    //######################################################
    LINFO("%s done!!!", PACKAGE);
    manager.stop();
    return 0;
} // end main


// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
