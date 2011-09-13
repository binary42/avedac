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

#include "Stages/UpdateEventsStage.H"
#include "MessagePassing/Mpimessage.H"
//#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/BitObject.H"
#include "Image/FilterOps.H"
#include "Image/Kernels.H"      // for twofiftyfives()
#include "Raster/Raster.H"
#include "Image/ColorOps.H"
#include "Image/MorphOps.H"
#include "Neuro/WTAwinner.H"
#include "Util/StringConversions.H"
#include "Utils/Version.H"
#include "PipelineControl/PipelineController.H"
#include "Utils/Const.H"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

using namespace std;

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

UpdateEventsStage::UpdateEventsStage(MPI_Comm mastercomm, const char *name,  \
                                     nub::soft_ref<InputFrameSeries> &ifs,  \
                                     nub::soft_ref<MbariResultViewer> &rv,  \
                                     const std::string& inputFileStem,  \
                                     const FrameRange &framerange)
: Stage(mastercomm, name),
itsAvgCache(DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache),
itsOutCache(DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache),
itsEventSet(DetectionParametersSingleton::instance()->itsParameters, inputFileStem),
itsInputFileStem(inputFileStem),
itsFrameRange(framerange),
itsSalientFrameCache(DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache),
itsFOEEst(20, 0),
itsLastEventSeedFrameNum(-1),
itsifs(ifs),
itsrv(rv) {

}

UpdateEventsStage::~UpdateEventsStage() {

}

void UpdateEventsStage::initStage() {
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

    itsifs->updateNext();

    // initialize the XML if requested to save events to XML
    if (itsrv->isSaveXMLEventsNameSet()) {
        Image< PixRGB<byte> > tmpimg;
        MbariImage< PixRGB<byte> > mstart(itsInputFileStem);
        MbariImage< PixRGB<byte> > mend(itsInputFileStem);

        // get the starting and ending timecodes from the frames
        nub::ref<FrameIstream> rep = itsifs->getFrameSource();

        rep->setFrameNumber(itsFrameRange.getFirst());
        tmpimg = rep->readRGB();
        mstart.updateData(tmpimg, itsFrameRange.getFirst());
 
        rep->setFrameNumber(itsFrameRange.getLast());
        tmpimg = rep->readRGB();
        mend.updateData(tmpimg, itsFrameRange.getLast());

        // create the XML document
        itsrv->createXMLDocument(PVersion::versionString(),
                itsFrameRange,
                mstart.getMetaData().getTC(),
                mend.getMetaData().getTC(),
                dp);

        // reset the frame number back to the beginning
        rep->setFrameNumber(itsFrameRange.getFirst());
    }
}

void UpdateEventsStage::runStage() {
    int exit = 0;
    int flag = 1;
    BitObject *obj;
    MPI_Status status;
    MPI_Request request;
    int frameNum = -1;
    Image< byte > *img;
    list<SalientWinner> *winners;

    LINFO("Running stage %s", Stage::name());

    do {

        if (probeMasterForExit())
            exit = 1;

        status.MPI_SOURCE = -1;
        status.MPI_TAG = -1;

        MPI_Iprobe(Stages::GSR_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &flag, &status);

        if (status.MPI_SOURCE == Stages::GSR_STAGE) {
            frameNum = receiveData((void**) &winners, SALIENTWINNERLIST, Stages::GSR_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
            Stages::stageID id = static_cast<Stages::stageID> (status.MPI_SOURCE);
            switch (status.MPI_TAG) {
                case(Stage::MSG_EXIT):
                    LDEBUG("%s received MSG_EXIT from: %s", Stage::name(), Stages::stageName(id));
                    exit = 1;
                    break;
                case(Stage::MSG_DATAREADY):
                    //if valid frame number store list in map
                    if (frameNum != -1) {

                        //map winner list to frameNum
                        itsWinners.insert(pair<int, list<SalientWinner>*>(frameNum, winners));

                        //last frame number used to seed event pool
                        itsLastEventSeedFrameNum = frameNum;

                        updateEvents();

                    }
                    else {
                        // skip to the next seed number
                        DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

                        itsLastEventSeedFrameNum += dp.itsSaliencyFrameDist;

                        updateEvents();
                    }
            }//end status.MPI_TAG
        }//end if status.MPI_SOURCE==GSR_STAGE

        status.MPI_SOURCE = -1;
        status.MPI_TAG = -1;

        MPI_Iprobe(Stages::SG_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &flag, &status);

        if (status.MPI_SOURCE == Stages::SG_STAGE) {
            frameNum = receiveData((void**) &img, BYTEIMAGE, Stages::SG_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
            Stages::stageID id = static_cast<Stages::stageID> (status.MPI_SOURCE);
            switch (status.MPI_TAG) {
                case(Stage::MSG_EXIT):
                    LDEBUG("%s received MSG_EXIT from: %s", Stage::name(), stageName(id));
                    exit = 1;
                    break;
                case(Stage::MSG_DATAREADY):
                    if (frameNum != -1) {
                        MbariImage< PixRGB<byte> > mbariRGBImg(itsInputFileStem);
                        Image< PixRGB<byte> > rgbimg;
                        MbariImage<byte> mbariBitImg(itsInputFileStem);
                        MPE_Log_event(7, 0, "");

                        // if the image sent is out of sync with the InputFrameSeries, something is wrong with the synchronization
                        if (frameNum != itsifs->frame())
                            LERROR("Error - input frame is out of sync between %s stage and %s stage", stageName(Stages::SG_STAGE), stageName(Stages::UE_STAGE));

                        // if send a frame number within range
                        if (frameNum <= itsFrameRange.getLast()) {
                            // store rgb image in cache; sometimes this fails due to NFS error so retry a few times
                            int ntrys = 0;
                            do {
                                rgbimg = itsifs->readRGB();
                            } while (!rgbimg.initialized() && ntrys++ < 3);

                            if (ntrys == 3)
                                LERROR("Error reading frame %06d after 3 tries", itsifs->frame());

                            // update the MBARI image wrapper with the rgbimage metadata
                            mbariRGBImg.updateData(rgbimg, frameNum);

                            // store input RGB image in itsRGBOutCache to use for video overlay
                            itsRGBOutCache.push_back(mbariRGBImg);

                            // store input RGB image ing itsAvgCache for use in color segmentation
                            itsAvgCache.push_back(mbariRGBImg);

                            // store received segmented bit image from Stages::SG_STAGE in itsOutCache
                            mbariBitImg.updateData(*img, frameNum);
                            itsOutCache.push_back(mbariBitImg);

                            // store frames used for seeding in itsSalientFrameCache
                            itsSalientFrameCache.push_back(mbariBitImg);

                            // update the frame counter
                            itsifs->updateNext();

                            // updateEvents using cached data
                            updateEvents();
                        }

                        LDEBUG("%s received frame: %d MSG_DATAREADY from: %s", Stage::name(), frameNum, stageName(id));
                        delete img;
                        MPE_Log_event(8, 0, "");
                    }
                    break;

                default:
                    LINFO("%s received frame: %d  MSG: %d from: %s", Stage::name(), frameNum, status.MPI_TAG, stageName(id));
                    break;
            }
        }
    } while (!exit && !probeMasterForExit());

}

void UpdateEventsStage::updateEvents() {
    // If a valid seed frame not yet initialized
    if (itsLastEventSeedFrameNum < 0) return;

    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    int numFrameDist = dp.itsSaliencyFrameDist;
    if (!itsOutCache.empty())
        LDEBUG(" outcacheframenum%d lasteventseed%d numframedist%d", itsOutCache.front().getFrameNum(), itsLastEventSeedFrameNum, numFrameDist);
    MbariImage< byte > mbariImg(itsInputFileStem.c_str());
    Image<byte> bitImgMasked;
    MbariImage< PixRGB<byte> > mbariRGBImg(itsInputFileStem.c_str());

    // keep updating events as long as within the range of seeded frames
    while (!itsOutCache.empty() && !itsRGBOutCache.empty() &&
            ((itsOutCache.front().getFrameNum() < (itsLastEventSeedFrameNum + numFrameDist)) ||
            (itsOutCache.front().getFrameNum() == itsFrameRange.getLast()))) {

        // update the MbariResultsViewer one frame - this assumes we are receiving sequential frames to process
        itsrv->updateNext();

        //pop off frames from oldest to newest
        mbariImg = itsOutCache.front();

        if (itsOutCache.front().initialized()) {
            LINFO("Processing frame %d lastseed:%d", mbariImg.getFrameNum(), itsLastEventSeedFrameNum);

            Image<byte> bitImg = itsOutCache.front();

            // update the focus of expansion
            Vector2D curFOE = itsFOEEst.updateFOE(bitImg);

            // cleanup image noise and display
            const Image<byte> se = twofiftyfives(dp.itsCleanupStructureElementSize);
            bitImg = erodeImg(dilateImg(bitImg, se), se);

            // mask special area in the frame we don't care
            bitImgMasked = maskArea(bitImg, &dp);

            itsrv->output(bitImgMasked, mbariImg.getFrameNum(), "Segment_output");

            //update the events using with the segmented binary image
            itsEventSet.updateEvents(bitImgMasked, curFOE, mbariImg.getFrameNum(), mbariImg.getMetaData());

            //if at next frame number for event seed, initiate events using cached winners
            if (mbariImg.getFrameNum() == itsLastEventSeedFrameNum) {
                initiateEvents(mbariImg.getFrameNum(), bitImgMasked);
            }

            // last frame? -> close everyone
            if (mbariImg.getFrameNum() == itsFrameRange.getLast())
                itsEventSet.closeAll();

            // weed out migit events (e.g. too few frames)
            itsEventSet.cleanUp(mbariImg.getFrameNum());
        }
        // are we loading the event structure from a file?
        const bool loadedEvents = itsrv->isLoadEventsNameSet();
        // are we loading the set of property vectors from a file?
        const bool loadedProperties = itsrv->isLoadPropertiesNameSet();
        const int circleRadius = mbariImg.getWidth() / 40;

        // initialize a few variables
        list<VisualEvent *> eventFrameList;
        list<VisualEvent *> eventListToSave;
        PropertyVectorSet pvs;

        if (!loadedEvents) {
            // get event frame list for this frame and those events that are ready to be saved
            // this is a list of all the events that have a token in this frame
            eventFrameList = itsEventSet.getEventsForFrame(mbariImg.getFrameNum());

            // this is a complete list of all those events that are ready to be written
            eventListToSave = itsEventSet.getEventsReadyToSave(mbariImg.getFrameNum());

            // write out eventSet?
            if (itsrv->isSaveEventsNameSet())
                itsrv->saveVisualEvent(itsEventSet, eventFrameList);

            if (itsrv->isSaveEventSummaryNameSet())
                itsrv->saveVisualEventSummary(PVersion::versionString(), eventListToSave);

            // flag events that have been saved for delete
            list<VisualEvent *>::iterator i;
            for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
                (*i)->flagWriteComplete();

            // write out positions?
            if (itsrv->isSavePositionsNameSet()) itsrv->savePositions(eventFrameList);

            PropertyVectorSet pvsToSave = itsEventSet.getPropertyVectorSetToSave();

            // write out property vector set?
            if (itsrv->isSavePropertiesNameSet()) itsrv->saveProperties(pvsToSave);
        }

        // do this only when we actually load frames
        if (itsrv->needFrames()) {
            mbariRGBImg = itsRGBOutCache.front();

            // get a list of events for this frame
            eventFrameList = itsEventSet.getEventsForFrame(mbariRGBImg.getFrameNum());

            // need to obtain the property vector set?
            if (!loadedProperties) pvs = itsEventSet.getPropertyVectorSet();

            // write out eventSet to XML?
            if (itsrv->isSaveXMLEventsNameSet())
                itsrv->saveVisualEventSetToXML(eventFrameList,
                    mbariRGBImg.getFrameNum(),
                    mbariRGBImg.getMetaData().getTC(),
                    itsFrameRange);
            // write results  ?
            if (itsrv->isSaveOutputSet()) {
                LINFO("Writing results for frame :%d", mbariRGBImg.getFrameNum());
                itsrv->outputResultFrame(mbariRGBImg,
                        itsEventSet,
                        circleRadius);
            }

            // need to save any event clips?
            if (itsrv->isSaveAllEventClips()) {
                //save all events
                LINFO("Saving event clips");
                list<VisualEvent *>::iterator i;
                for (i = eventFrameList.begin(); i != eventFrameList.end(); ++i) {
                    itsrv->saveSingleEventFrame(mbariRGBImg, mbariRGBImg.getFrameNum(), *i);
                }
            } else { //only save enumerated list of events (only for post-process clip creation)
                uint csavenum = itsrv->numSaveEventClips();
                for (uint idx = 0; idx < csavenum; ++idx) {
                    uint evnum = itsrv->getSaveEventClipNum(idx);
                    if (!itsEventSet.doesEventExist(evnum)) continue;

                    VisualEvent *event = itsEventSet.getEventByNumber(evnum);
                    if (event->frameInRange(itsOutCache.front().getFrameNum()))
                        itsrv->saveSingleEventFrame(itsRGBOutCache.front(), itsRGBOutCache.front().getFrameNum(), event);
                }
            }// end of saving enumerated list of events

            LINFO("########## %d  ##### %d", mbariImg.getFrameNum(), itsFrameRange.getLast());

            // if last frame, send exit signal to controller
            if (mbariImg.getFrameNum() == itsFrameRange.getLast()) {
                int flag = 1;
                LINFO("SHUTDOWN %s sending message MASTER_SHUTDOWN to Controller", Stage::name());
                MPI_Send(&flag, 1, MPI_INT, Stages::CONTROLLER, MASTER_SHUTDOWN, Stage::mastercomm());
            }
            // clean up Caches and vector sets
            if (!itsOutCache.empty())
                itsOutCache.pop_front();
            if (!itsRGBOutCache.empty())
                itsRGBOutCache.pop_front();
        }// end if need frames

        if (!loadedEvents) {
            // flag events that have been saved for delete otherwise takes too much memory
            list<VisualEvent *>::iterator i;
            for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
                (*i)->flagForDelete();

            // clean up event list
            eventFrameList.clear();
            eventListToSave.clear();
        }
        if (itsOutCache.empty() || itsRGBOutCache.empty()) break;
    }
}

void UpdateEventsStage::initiateEvents(int frameNum, Image< byte > bitImg) {
    int index = frameNum - itsLastEventSeedFrameNum;

    if (!itsSalientFrameCache.empty() && index < itsSalientFrameCache.size()) {
        map<int, list<SalientWinner> *>::iterator iter = itsWinners.find(frameNum);

        // if itsWinners map list has valid winner
        if (iter != itsWinners.end()) {

            // get list of winners for this frameNum
            list<SalientWinner>* winners = itsWinners[frameNum];

            // convert SalientWinner to WTAWinner object
            std::list<SalientWinner>::const_iterator i = winners->begin();
            std::list<WTAwinner> wtawinners;
            while (i != winners->end()) {
                wtawinners.push_back(WTAwinner(i->itsWinner, SimTime::ZERO(), i->itsWinnerSMV, false));
                i++;
            }

            // extract salient BitObjects
            LINFO("Extracting salient BitObjects for frame: %d number of potential winners: %d", frameNum, wtawinners.size());
            list<BitObject> sobjs = getSalientObjects(bitImg, wtawinners); 

            // initiate events with these objects
            LINFO("Initiating events for frame %d number of objects found %d number winners: %d", frameNum, sobjs.size(), winners->size());
            MbariImage <byte> mbariImg = itsSalientFrameCache[index];
            itsEventSet.initiateEvents(sobjs, frameNum, mbariImg.getMetaData());

            //cleanup allocated list
            winners->clear();
            delete winners;
            itsWinners.erase(frameNum);

        }// end if map list has valid winner
    }// end if frameNum < itsSalientFrameCache.size()

    //last frame number used to seed event pool. some frames have no winners so reset this every time called
    while (!itsSalientFrameCache.empty() && itsSalientFrameCache.front().getFrameNum() <= frameNum)
        itsSalientFrameCache.pop_front();
}

void UpdateEventsStage::shutdown() {
}
// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
