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
#include "Stages/SegmentStage.H"
#include "PipelineControl/PipelineController.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/FilterOps.H"
#include "Image/MathOps.H"
#include "Image/ColorOps.H"
#include "Image/Transforms.H"
#include "Image/ImageCache.H"
#include "Image/MbariImage.H"

#include "Raster/RasterFileFormat.H"
#include "MessagePassing/Mpidef.H"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

SegmentStage::SegmentStage(MPI_Comm mastercomm, const char *name,
        nub::soft_ref<InputFrameSeries> &ifs,
        nub::soft_ref<MbariResultViewer> &rv,
        const FrameRange framerange,
        const std::string& inputFileStem)
: Stage(mastercomm, name),
itsifs(ifs),
itsRv(rv),
itsFrameRange(framerange),
itsInputFileStem(inputFileStem) {
}

SegmentStage::~SegmentStage() {

}

void SegmentStage::runStage() {
    int exit = 0;
    int flag = 1;
    MPI_Status status;
    MPI_Request request;
    Image< byte > *img2segment;
    int framenum = -1;
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    Segmentation segmentation;
    // get graph parameters
    vector<float> p = segmentation.getFloatParameters(dp.itsSegmentGraphParameters);
    const float sigma = segmentation.getSigma(p);
    const int k = segmentation.getK(p);
    const int min_size = segmentation.getMinSize(p);
    const int offset = dp.itsSegmentAdaptiveOffset;
    const int maxDist = dp.itsMaxDist;

    LINFO("Running stage %s", Stage::name());

    do {
        framenum = receiveData((void**) &img2segment, BYTEIMAGE, Stages::CP_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
        Stages::stageID id = static_cast<Stages::stageID> (status.MPI_SOURCE);
        LDEBUG("%s received frame: %d MSG_DATAREADY from: %s", Stage::name(), framenum, Stages::stageName(Stages::CP_STAGE));

        switch (status.MPI_TAG) {
            case(Stage::MSG_EXIT):
                LDEBUG("%s received MSG_EXIT from %s", Stage::name(), Stages::stageName(id));
                exit = 1;
                break;
            case(Stage::MSG_DATAREADY):
                MPE_Log_event(3, 0, "");

                if (framenum != -1) {
                   
                    // create a binary image for the segmentation
                    Image<byte> bitImgShadow, bitImgHighlight;
                    Image< PixRGB<byte > > graphBitImg;
                    Image<byte> bitImg(img2segment->getDims(), ZEROS);

                    graphBitImg = segmentation.runGraph(sigma, k, min_size, *img2segment);
                    std::list<WTAwinner> winlistGraph = getGraphWinners(graphBitImg, framenum, 1.0f, 1.0f);
                    list<BitObject> sobjs = getSalientObjects(graphBitImg, winlistGraph);

                    if (dp.itsSegmentAlgorithmType == SAGraphCutOnly) {
                        if (sobjs.size() > 0)
                            bitImg = showAllObjects(sobjs);
                    }
                    else if (dp.itsSegmentAlgorithmType == SAMeanAdaptiveThreshold) {
                        bitImgShadow = segmentation.mean_thresh(*img2segment, maxDist, offset);

                        itsRv->output(bitImgShadow, framenum, "Segment_meanshadow");

                        if (sobjs.size() > 0) {
                            bitImgHighlight = showAllObjects(sobjs);
                            itsRv->output(bitImgHighlight, framenum, "Segment_highlight");
                            bitImg = bitImgHighlight + bitImgShadow;
                        } else {
                            bitImg = bitImgShadow;
                        }
                    }
                    else if (dp.itsSegmentAlgorithmType == SAMedianAdaptiveThreshold) {
                        bitImgShadow = segmentation.median_thresh(*img2segment, maxDist, offset);

                        itsRv->output(bitImgShadow, framenum, "Segment_medianshadow");

                        if (sobjs.size() > 0) {
                            bitImgHighlight = showAllObjects(sobjs);
                            itsRv->output(bitImgHighlight, framenum, "Segment_highlight");
                            bitImg = bitImgHighlight + bitImgShadow;
                        } else {
                            bitImg = bitImgShadow;
                        }
                    }
                    else if (dp.itsSegmentAlgorithmType == SAMeanMinMaxAdaptiveThreshold) {
                        bitImgShadow = segmentation.meanMaxMin_thresh(*img2segment, maxDist, offset);

                        itsRv->output(bitImgShadow, framenum, "Segment_meanMinMaxShadow");

                        if (sobjs.size() > 0) {
                            bitImgHighlight = showAllObjects(sobjs);
                            itsRv->output(bitImgHighlight, framenum, "Segment_highlight");
                            bitImg = bitImgHighlight + bitImgShadow;
                        } else {
                            bitImg = bitImgShadow;
                        }
                    }

                    itsRv->output(graphBitImg, framenum, "Graph_segment");
                    
                    //send byte image to UpdateEvents stage to start work
                    sendByteImage(bitImg, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
                    delete img2segment;
                    MPE_Log_event(4, 0, "");

                }
                break;
            default:
                LDEBUG("%s received %d frame: %d from: %s", Stage::name(), framenum, status.MPI_TAG, stageName(id));
                break;
        }
    } while (!exit && !probeMasterForExit());

    //UE may be pending on message from CP stage; send EXIT message to interrupt
    MPI_Isend(&flag, 1, MPI_INT, Stages::UE_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &request);
}

void SegmentStage::shutdown() {
}


