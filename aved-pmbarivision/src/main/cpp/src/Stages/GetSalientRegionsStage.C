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

#include "MessagePassing/Mpimessage.H" 
#include "Stages/GetSalientRegionsStage.H"
#include "PipelineControl/PipelineController.H"
#include "Image/BitObject.H"
#include "Neuro/WTAwinner.H"
#include "Neuro/NeuroSimEvents.H"
#include "Media/MediaSimEvents.H"
#include "Media/SimFrameSeries.H"
#include "Raster/Raster.H"
#include "Image/ColorOps.H"
#include "Image/fancynorm.H"
#include "Simulation/SimEventQueue.H"
#include "Simulation/SimEvents.H"
#include "Util/StringConversions.H"
#include "Data/MbariOpts.H"
#include "Parallel/pvisionTCP-defs.H"
#include "Util/Timer.H"
#include "Image/Image.H"
#include "Image/ColorOps.H"
#include "Image/DrawOps.H"
#include "Image/ShapeOps.H"
#include "Image/FilterOps.H"
#include "Image/MathOps.H"
#include "Image/Transforms.H"
#include "Image/ImageSet.H"
#include "Raster/RasterFileFormat.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/colorDefs.H"
#include "Stages/SalientWinner.H"
#include "Utils/Const.H"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <iostream>
#include <string>
#include <iostream>
#define SIGMA 20

using namespace std;

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

GetSalientRegionsStage::GetSalientRegionsStage(MPI_Comm mastercomm, const char *name,  \
                                               const int argc, const char **argv,
        nub::soft_ref<Beowulf> &beo,
        nub::soft_ref<WinnerTakeAll> &wta,
	nub::soft_ref<SaliencyMapStd> &sm,
	nub::soft_ref<AttentionGuidanceMapStd> &agm,
        nub::soft_ref<SimEventQueue> &seq,
        const ShapeEstimatorMode &sem,
 	const int foaRadius,
        const LevelSpec &levelSpec,
        const float boringmv,
        const SimTime &boringDelay,
        const MaxNormType &maxNormType,
        const VisualCortexWeights &wts,
        const float scaleW,
        const float scaleH)
: Stage(mastercomm, name),
itsArgc(argc),
itsArgv(argv),
itsBeo(beo),
itsWta(wta),
itsSm(sm),
itsAgm(agm),
itsSeq(seq),
itsSem(sem),
itsFoaRadius(foaRadius),
itsLevelSpec(levelSpec),
itsMaxNormType(maxNormType),
itsBoringmv(boringmv),
itsBoringDelay(boringDelay),
itsWeights(wts)
{
    if (scaleW > 0.f)
        itsScaleW = scaleW;
    else
        itsScaleW = 1.0f;

    if (scaleH > 0.f)
        itsScaleH = scaleH;
    else
        itsScaleH = 1.0f;

}

GetSalientRegionsStage::~GetSalientRegionsStage() {
}

void GetSalientRegionsStage::initStage() {
    if (!itsBeo->started())
        itsBeo->start();
}

void GetSalientRegionsStage::runStage() {
    int exit = 0;
    Image< PixRGB<byte> > *img;
    Image< PixRGB<byte> > img2runsaliency;
    MPI_Status status;
    MPI_Request request;
    int framenum = -1;

    LINFO("Running stage %s", Stage::name());

    do {

        framenum = receiveData((void**) &img, RGBBYTEIMAGE, Stages::CP_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
        Stages::stageID id = static_cast<Stages::stageID> (status.MPI_SOURCE);
        LDEBUG("%s received frame: %d MSG_DATAREADY from Source: %d", Stage::name(), framenum, status.MPI_SOURCE);

        switch (status.MPI_TAG) {
            case(Stage::MSG_EXIT):
                LDEBUG("%s received MSG_EXIT from %s", Stage::name(), Stages::stageName(id));
                exit = 1;
                break;
            case(Stage::MSG_DATAREADY):

                // get saliency winners and send list of winners to the Stages::UE_STAGE
                if (framenum != -1) {
                    MPE_Log_event(5, 0, "");

                    img2runsaliency = *img; 

                    // send the image to the beowulf worker nodes
                    sendImage(img2runsaliency, framenum);

                    // get the winners back
                    std::list<WTAwinner> winners = getWinners(img2runsaliency, framenum);
                    std::list<SalientWinner> salwinners;

                    // initialize salwinners list for sending through mpi message to US_STAGE
                    if (winners.size() > 0) {
                        std::list<WTAwinner>::iterator i;

                        for (i = winners.begin(); i != winners.end(); ++i)
                            salwinners.push_back(SalientWinner(i->p, i->sv));


                        // send winner list to Stages::UE_STAGE
                        sendSalientWinnerList(salwinners, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
                        salwinners.clear();
                    } else {
                        // send empty winner list to Stages::UE_STAGE
                        sendSalientWinnerList(salwinners, -1, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
                    }

                    delete img;
                    winners.clear();
                    MPE_Log_event(6, 0, "");
                }
                break;
            default:
                LDEBUG("%s received frame: %d  MSG: %d from Source: %d", Stage::name(), framenum,
                        status.MPI_TAG, id);
                break;
        }
    } while (!exit && !probeMasterForExit());

}

void GetSalientRegionsStage::shutdown() {
    int flag = 1;
    MPI_Request request;

    //Stages::UE_STAGE may be pending on message from GSR stage; send EXIT message to interrupt
    MPI_Isend(&flag, 1, MPI_INT, Stages::UE_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &request);

    if (itsBeo->started())
        itsBeo->stop();
}

// ######################################################################

std::list<WTAwinner> GetSalientRegionsStage::getSalientWinners(const Image< PixRGB<byte> > &img,
        int framenum,
        int sml,
        Image<float> &sm,
        Image<float> &intensity,
        Image<float> &color,
        Image<float> &orientation) {
    Point2D<int> currwin(-1, -1);
    int numSpots = 0;
    std::list<WTAwinner> winners;
    SimStatus status = SIM_CONTINUE;
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

    // initialize the max time to simulate 
    const SimTime simMaxEvolveTime = SimTime::MSECS(itsSeq->now().msecs()) + SimTime::MSECS(dp.itsMaxEvolveTime);

    rutz::shared_ptr<SimEventVisualCortexOutput> e(new SimEventVisualCortexOutput(NULL, sm)); 
    itsSeq->post(e);

    while (status == SIM_CONTINUE) {

        itsSm->evolve(*itsSeq);
        itsAgm->evolve(*itsSeq);
        itsWta->evolve(*itsSeq);
        
	// switch to next time step:
        status = itsSeq->evolve();

        if (SeC<SimEventWTAwinner> e = itsSeq->check<SimEventWTAwinner > (0)) {
            WTAwinner newwin = e->winner();
	    currwin = newwin.getSMcoords(sml);
            newwin.p.i = (int) ((float) newwin.p.i * itsScaleW);
            newwin.p.j = (int) ((float) newwin.p.j * itsScaleH); 

            LINFO("#### winner #%d found at [%d; %d] with %f voltage frame: %d ",
                    numSpots, newwin.p.i, newwin.p.j, newwin.sv, framenum);

            // if a boring event detected, and not keeping boring WTA points then break simulation
            if (newwin.boring && dp.itsKeepWTABoring == false) {
                rutz::shared_ptr<SimEventBreak>
                        e(new SimEventBreak(0, "Boring event detected"));
                itsSeq->post(e);
            } else {
                winners.push_back(newwin);
                ++numSpots;
	    }

            if (numSpots >= dp.itsMaxWTAPoints) {
              LINFO("#### found maximum number of saliency spots %d", dp.itsMaxWTAPoints);
              rutz::shared_ptr<SimEventBreak>
                  e(new SimEventBreak(0, "##### found maximum number of salient spots #####"));
              itsSeq->post(e);
            }
			
	    LINFO("##### time now:%f msecs max evolve time:%f msecs frame: %d #####", itsSeq->now().msecs(), simMaxEvolveTime.msecs(), framenum);

	    if (itsSem != SEMnone) { 
               // scan channels, finding the max
                float mx = -1.0F;
                int bestindex = -1;
                for (uint i = 0; i < 3; ++i) {
                    if (currwin.i != -1) {
                        float curr_val = 0.0f;
                        switch (i) {
                            case(0):
                                curr_val = intensity.getVal(currwin);
                                break;
                            case(1):
                                curr_val = color.getVal(currwin);
                                break;
                            case(2):
                                curr_val = orientation.getVal(currwin);
                                break;
                            default:
                                curr_val = 0.0f;
                                break;
                        }
                        // do we have a new max?
                        if (curr_val >= mx) {
                            mx = curr_val;
                            bestindex = i;
                        }
                    }
                }
		std::string winlabel;

                //mask max object
                if (bestindex > -1) {
                    Image<float> winMap; 
                    switch (bestindex) {
                        case(0):
			    winlabel = "Intensity";
                            winMap = intensity;
                            break;
                        case(1):
			    winlabel = "Color";
                            winMap = color;
                            break;
                        case(2):
			    winlabel = "Orientation";
                            winMap = orientation;
                            break;
                        default:
			    winlabel = "Orientation";
                            winMap = orientation;
                            break;
	        	} 
			const bool goodseed = (winMap.getVal(currwin) > 0.0F);
                        Image<float> winMapNormalized = winMap; 
			inplaceNormalize(winMapNormalized, 0.0F, 1.0F); 
			Image<byte> objectMask;
			Dims indims = img.getDims();

			// if we found a good seed point, use it to segment the
      			// object. Otherwise, we failed, so let's just return a disk:
      			if (goodseed)
        		{
          			LDEBUG("Segmenting object around (%d, %d) in %s.",
                 		newwin.p.i, newwin.p.j, winlabel.c_str());
          			objectMask = segmentObjectClean(winMapNormalized, currwin);
        		}
      			else
        		{
          			LDEBUG("Drawing disk object around (%d, %d) in %s.",
                 		newwin.p.i, newwin.p.j, winlabel.c_str());
          			objectMask.resize(winMap.getDims(), true);
          			drawDisk(objectMask, currwin,
                   		(itsFoaRadius * objectMask.getWidth()) / indims.w(), byte(255));
        		}


			Image<byte> objMask2(objectMask); 
			objMask2.setVal((newwin.p.i * objectMask.getWidth()) / indims.w(), (newwin.p.j * objectMask.getHeight()) / indims.h(), byte(255));
      		 	Image<byte> iorMask = lowPass3(objMask2) * (winMapNormalized + 0.25F);
	
			// inplaceSetValMask(sm, iorMask, 0.0F); 
			Image<float> temp = scaleBlock(objectMask, indims);
            		itsSmoothMask = convGauss<float>(temp, SIGMA, SIGMA, 5);
            		inplaceNormalize(itsSmoothMask, 0.0F, 3.0F);
            		inplaceClamp(itsSmoothMask, 0.0F, 1.0F);

      			// update the cumulative smooth mask:
      			if (itsCumMask.initialized())
        		   itsCumMask = takeMax(itsCumMask, itsSmoothMask);
      			else
        		   itsCumMask = itsSmoothMask;

      			rutz::shared_ptr<SimEventShapeEstimatorOutput>
        		e(new SimEventShapeEstimatorOutput(NULL, winMap, objectMask, iorMask,
                                          itsSmoothMask, itsCumMask,
                                          winlabel, true));
      			itsSeq->post(e);
			}
		}
        }

        if (itsSeq->now().msecs() >= simMaxEvolveTime.msecs()) {
            LINFO("##### time limit reached time now:%f msecs max evolve time:%f msecs #####", itsSeq->now().msecs(), simMaxEvolveTime.msecs());
            rutz::shared_ptr<SimEventBreak>
                    e(new SimEventBreak(0, "##### time limit reached #####"));
            itsSeq->post(e);
        }
    }

    // print final memory allocation stats
    LINFO("Simulation terminated. Found %d numspots", numSpots);

    return winners;
}

list<WTAwinner> GetSalientRegionsStage::getWinners(const Image< PixRGB<byte> > &img, int framenum) {
    std::list<WTAwinner> winners;
    Image<float> cmap[NBCMAP]; // array of conspicuity maps
    int32 cmapframe[NBCMAP]; // array of cmap frame numbers
    for (int i = 0; i < NBCMAP; i++) cmapframe[i] = -1;
    int sml = itsLevelSpec.mapLevel(); // pyramid level of saliency map
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

    Image<float> sm(img.getWidth() >> sml, img.getHeight() >> sml, ZEROS); // saliency map
    Image<float> color = sm;
    Image<float> intensity = sm;
    Image<float> ori = sm;

    int reccmaps = 0;
    int numcmaps = NBCMAP;
    Timer masterclock; // master clock for simulations
    masterclock.reset();
    float mi, ma;
 
    // adjust expected number of channels based on channel weight for
    // intensity, color, or orientation 
    if (itsWeights.chanIw == 0.f)
        numcmaps -= 1;

    if (itsWeights.chanOw == 0.f)
        numcmaps -= 4;

    if (itsWeights.chanCw == 0.f)
        numcmaps -= 2;

    // receive conspicuity maps:
    while (reccmaps < numcmaps) {
        reccmaps += receiveCMAPS(itsBeo, cmap, cmapframe);
        usleep(500);
    };

    if (itsWeights.chanOw != 0.f) {
        // add all the orientation channels together, max normalize and weight
        for (int i = 3; i < NBCMAP; i++) {
            LDEBUG("sml: %d image: %dx%d ori: %dx%d cmap: %dx%d", sml, img.getWidth(), img.getHeight(), ori.getWidth(), ori.getHeight(), cmap[i].getWidth(), cmap[i].getHeight());
            ori += cmap[i];
        }
        ori = maxNormalize(ori, 0.0f, 0.0f, itsMaxNormType);
        getMinMax(ori, mi, ma);
        LDEBUG("Orientation final range [%f .. %f]", mi, ma);
        ori *= itsWeights.chanOw;
        getMinMax(ori, mi, ma);
        LDEBUG("Orientation final %f weighted range [%f .. %f]", itsWeights.chanOw, mi, ma);
    }

    // add in the r/g b/w color map computations if a color image
    if (itsWeights.chanCw != 0.f) {
        // add all the color channels together, max normalize and weight
        color = cmap[1] + cmap[2];
        color = maxNormalize(color, 0.0f, 0.0f, itsMaxNormType);
        getMinMax(color, mi, ma);
        LDEBUG("Color final range [%f .. %f]", mi, ma);
        color *= itsWeights.chanCw;
        getMinMax(color, mi, ma);
        LDEBUG("Color final %f weighted range [%f .. %f]", itsWeights.chanCw, mi, ma);
    }

    if (itsWeights.chanIw != 0.f) {
        // maxnormalize and weight intensity channel
        intensity = cmap[0];
        maxNormalize(intensity, 0.0f, 0.0f, itsMaxNormType);
        getMinMax(intensity, mi, ma);
        LDEBUG("Intensity final range [%f .. %f]", mi, ma);
        intensity *= itsWeights.chanIw;
        getMinMax(intensity, mi, ma);
        LDEBUG("Intensity final %f weighted range [%f .. %f]", itsWeights.chanIw, mi, ma);
    }

    // build our current saliency map
    Image<float> sminput = ori + color + intensity;

    getMinMax(sminput, mi, ma);
    LDEBUG("Raw input range is [%f .. %f]", mi, ma);
    sminput = maxNormalize(sminput, 0.f, 2.f, itsMaxNormType);

    // output is     now typically in the (0.0..8.0) range;
    // typical images are in (0..4) range; we want input current in nA
    sminput *= 1e-9F;
    getMinMax(sminput, mi, ma);
    LINFO("Salmap input range is [%f .. %f] nA", mi * 1.0e9F, ma * 1.0e9F);

    // inject saliency map input into saliency map:
    if (sminput.initialized())
        sm = sm * 0.7F + sminput * 0.3F;

    int minframe = -1;
    // check for accumulated delay in pvision slaves
    for (int i = 1; i < NBCMAP; i++) {
        LDEBUG("cmap: %d ", cmapframe[i]);
        if (cmapframe[i] != -1 && (cmapframe[i] < minframe) || minframe == -1)
            minframe = cmapframe[i];
    }

    // this is the frame number that corresponds to the oldest frame
    if ((framenum - minframe) > 10)
        LINFO("ERROR: SENDING FRAMES TOO FAST framenum: %d minframe:%d", framenum, minframe);

    // get the standard deviation in the input image
    // if there is little deviation, this image is uniform and
    // will have no saliency so return empty winners
    if (dp.itsMinStdDev > 0.f) {
        float stddevlum = stdev(luminance(img));
        // get the standard deviation in the input image
        // if there is no deviation, this image is uniform and
        // will have no saliency so return empty winners
        if (stddevlum <= dp.itsMinStdDev) {
            LINFO("##### standard deviation in luminance: %f less than or equal to minimum: %f. No winners will be computed !!#####", stddevlum, dp.itsMinStdDev);
            return std::list<WTAwinner > ();
        } else {
            LINFO("##### standard deviation in luminance: %f#####", stddevlum);
        }
    }

    winners = getSalientWinners(img, framenum, sml, sm, intensity,color,ori);

    return winners;
}

void GetSalientRegionsStage::sendImage(const Image< PixRGB<byte> >& img, int framenum) {
    // buffer to send messages to nodes
    TCPmessage smsg; 

    // compute luminance and send it off:
    Image<byte> lum = luminance(img);

    if (itsWeights.chanOw != 0.f) {
        LDEBUG("######## sending luminance to BEO_45,BEO_90,BEO_135");
        // first, send off luminance to orientation slaves:
        smsg.reset(framenum, BEO_ORI0);
        smsg.addImage(lum);
        itsBeo->send(smsg);
        smsg.setAction(BEO_ORI45);
        itsBeo->send(smsg);
        smsg.setAction(BEO_ORI90);
        itsBeo->send(smsg);
        smsg.setAction(BEO_ORI135);
        itsBeo->send(smsg);
    }

    if (itsWeights.chanIw != 0.f) {
        LDEBUG("####### sending luminance to BEO_LUMINANCE");
        // finally, send to luminance slave:
        smsg.setAction(BEO_LUMINANCE);
        itsBeo->send(smsg);
    }

    if (itsWeights.chanCw != 0.f) {
        LDEBUG("######### sending luminance to BEO_REDGREEN BEO_BLUEYELLOW");
        // compute RG and BY and send them off:
        Image<byte> r, g, b, y;
        getRGBY(img, r, g, b, y, (byte) 25);
        smsg.reset(framenum, BEO_REDGREEN);
        smsg.addImage(r);
        smsg.addImage(g);
        itsBeo->send(smsg);
        smsg.reset(framenum, BEO_BLUEYELLOW);
        smsg.addImage(b);
        smsg.addImage(y);
        itsBeo->send(smsg);
    }
}
// ######################################################################

int GetSalientRegionsStage::receiveCMAPS(nub::soft_ref<Beowulf>& beo, Image<float> *cmap,
        int32 *cmapframe) {
    //TODO: Add timeout for cmap receive
    TCPmessage rmsg; // buffer to receive messages from nodes
    int32 rframe, raction, rnode = -1, recnb = 0, reccmaps = 0; // receive from any node

    while (beo->receive(rnode, rmsg, rframe, raction)) {
        LDEBUG("received %d/%d from %d", rframe, raction, rnode);
        switch (raction & 0xffff) {
            case BEO_CMAP: // ##############################
            {
                // get the map:
                Image<float> ima = rmsg.getElementFloatIma();

                // the map number is stored in the high 16 bits of the
                // raction field:
                int32 mapn = raction >> 16;
                if (mapn < 0 || mapn >= NBCMAP) {
                    LERROR("Bogus cmap number ignored");
                    break;
                }

                // here is a totally asynchronous system example: we
                // just update our current value of a given cmap if
                // the one we just received is more recent than the
                // one we had so far:
                if (cmapframe[mapn] < rframe) {
                    cmap[mapn] = ima;
                    cmapframe[mapn] = rframe;
                    reccmaps++;
                    LDEBUG("rframe: %d  mapnum: %d reccmaps: %d", rframe, mapn, reccmaps);
                }
            }

                break;
            default: // ##############################
                LERROR("Bogus action %d -- IGNORING.", raction);
                break;
        }
        // limit number of receives, so we don't hold CPU too long:
        recnb++;
        if (recnb > 3) break;
    }

    return reccmaps;
}
