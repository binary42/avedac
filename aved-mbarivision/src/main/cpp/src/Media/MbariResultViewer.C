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

#include "Media/MbariResultViewer.H"

#include "Component/ModelManager.H"
#include "Component/ModelOptionDef.H"
#include "GUI/XWinManaged.H"
#include "Image/DrawOps.H"
#include "Image/CutPaste.H"  // for crop()
#include "Image/Image.H"
#include "Image/ShapeOps.H"  // for rescale()
#include "Image/Transforms.H"
#include "Image/colorDefs.H"
#include "Image/SimpleFont.H"
#include "Util/Assert.H"
#include "Util/log.H"

#include "Data/MbariOpts.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Media/FrameSeries.H"
#include "Utils/Version.H"
#include "Raster/GenericFrame.H"
#include "Transport/FrameInfo.H"

#include <xercesc/util/OutOfMemoryException.hpp>
#include <xercesc/parsers/XercesDOMParser.hpp>
#include <xercesc/framework/LocalFileFormatTarget.hpp>
#include <xercesc/dom/DOM.hpp>
#include <sstream>
#include <cstdio>
#include <fstream>

// #############################################################################

MbariResultViewer::MbariResultViewer(ModelManager& mgr,
        nub::soft_ref<OutputFrameSeries> evtofs,
        nub::soft_ref<OutputFrameSeries> ofs,
        string binDir)
: ModelComponent(mgr, string("MbariResultViewer"),
        string("MbariResultViewer")),
itsSaveResults(&OPT_MRVsaveResults, this),
itsDisplayResults(&OPT_MRVdisplayResults, this),
itsMarkInteresting(&OPT_MRVmarkInteresting, this),
itsOpacity(&OPT_MRVopacity, this),
itsMarkCandidate(&OPT_MRVmarkCandidate, this),
itsMarkPrediction(&OPT_MRVmarkPrediction, this),
itsMarkFOE(&OPT_MRVmarkFOE, this),
itsSaveOutput(&OPT_MRVsaveOutput, this),
itsDisplayOutput(&OPT_MRVdisplayOutput, this),
itsShowEventLabels(&OPT_MRVshowEventLabels, this),
itsRescaleDisplay(&OPT_MRVrescaleDisplay, this),
itsSizeAvgCache(&OPT_MDPsizeAvgCache, this),
itsSaveEventsName(&OPT_MRVsaveEvents, this),
itsLoadEventsName(&OPT_MRVloadEvents, this),
itsSavePropertiesName(&OPT_MRVsaveProperties, this),
itsLoadPropertiesName(&OPT_MRVloadProperties, this),
itsSavePositionsName(&OPT_MRVsavePositions, this),
itsSaveEventNumString(&OPT_MRVsaveEventNums, this),
itsSaveSummaryEventsName(&OPT_MRVsaveSummaryEventsName, this),
itsSaveXMLEventSetName(&OPT_MRVsaveXMLEventSet, this),
itsMetadataSource(&OPT_MRVmetadataSource, this),
resFrameWindow(NULL),
colInteresting(COL_INTERESTING),
colCandidate(COL_CANDIDATE),
colPrediction(COL_PREDICTION),
colFOE(COL_FOE),
itsEvtOfs(evtofs),
itsOfs(ofs),
XMLfileCreated(false),
appendEvents(false),
appendEventSummary(false),
appendEventsXML(false),
appendProperties(false),
itsSaveEventNumsAll(false) {
    itsXMLParser = new MbariXMLParser(binDir);
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    itsSaveNonInterestingEvents = p.itsSaveNonInteresting;
}

void MbariResultViewer::updateNext() {
    itsOfs->updateNext();
    itsEvtOfs->updateNext();
}

MbariResultViewer::~MbariResultViewer() {
    // destroy everything
    freeMem();
}

// ######################################################################

void MbariResultViewer::paramChanged(ModelParamBase * const param,
        const bool valueChanged,
        ParamClient::ChangeStatus* status) {
    ModelComponent::paramChanged(param, valueChanged, status);

    // if the param is out itsMarkCandidate set the color accordingly
    // if the param is set to Save all non-interesting events, mark candidates
    // the same as interesting to be less confusing
    if (param == &itsMarkCandidate) {
        if (itsMarkCandidate.getVal() && itsSaveNonInterestingEvents) 
  	  colCandidate = COL_INTERESTING;
	else if (itsMarkCandidate.getVal() && !itsSaveNonInterestingEvents) 
	  colCandidate = COL_CANDIDATE;
        else colCandidate = COL_TRANSPARENT;
    }
        // if the param is out itsMarkSkip set the color accordingly
    else if (param == &itsMarkPrediction) {
        if (itsMarkPrediction.getVal()) colPrediction = COL_PREDICTION;
        else colPrediction = COL_TRANSPARENT;
    }
        // if the param is out itsMarkSkip set the color accordingly
    else if (param == &itsMarkFOE) {
        if (itsMarkFOE.getVal()) colFOE = COL_FOE;
        else colFOE = COL_TRANSPARENT;
    }
        // if the param is itsSaveEventNum, parse the string and fill the vector
    else if (param == &itsSaveEventNumString)
        parseSaveEventNums(itsSaveEventNumString.getVal());
}

// ######################################################################

void MbariResultViewer::reset1() {
    // destroy our stuff
    freeMem();

    // propagate to our base class:
    ModelComponent::reset1();
}

// ######################################################################

void MbariResultViewer::freeMem() {
    for (uint i = 0; i < itsResultWindows.size(); ++i)
        if (itsResultWindows[i] != NULL) delete itsResultWindows[i];

    if (resFrameWindow != NULL) delete resFrameWindow;

    itsResultNames.clear();
    itsResultWindows.clear();

    itsSaveEventsName.setVal("");
    itsLoadEventsName.setVal("");
    itsSavePropertiesName.setVal("");
    itsLoadPropertiesName.setVal("");

    itsSaveSummaryEventsName.setVal("");
    itsLoadEventsName.setVal("");
    itsSaveXMLEventSetName.setVal("");

}

// #############################################################################

template <class T>
void MbariResultViewer::output(const Image<T>& img, const uint frameNum,
const string& resultName, const int resNum) {
    if (itsDisplayResults.getVal()) display(img, frameNum, resultName, resNum);
    if (itsSaveResults.getVal()) save(img, frameNum, resultName, resNum);
}

// #############################################################################

template <class T>
void MbariResultViewer::display(const Image<T>& img, const uint frameNum,
const string& resultName, const int resNum) {
    uint num = getNumFromString(resultName);
    itsResultWindows[num] = displayImage(img, itsResultWindows[num],
            getLabel(num, frameNum, resNum).c_str());
}

// #############################################################################

void MbariResultViewer::save(const Image< PixRGB<byte> >& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void MbariResultViewer::save(const Image<byte>& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void MbariResultViewer::save(const Image<float>& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img, FLOAT_NORM_0_255), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void MbariResultViewer::outputResultFrame(MbariImage< PixRGB<byte> >& resultImg,
        MbariVisualEvent::VisualEventSet& evts,
        const int circleRadius) {
    MbariMetaData m = resultImg.getMetaData();
    Image< PixRGB<byte> > final_image = resultImg;

    if (itsDisplayOutput.getVal() || itsSaveOutput.getVal()) {
        evts.drawTokens(final_image, resultImg.getFrameNum(), circleRadius,
                itsMarkInteresting.getVal(), itsOpacity.getVal(),
                colInteresting, colCandidate, colPrediction,
                colFOE,
                itsShowEventLabels.getVal(),
                itsSaveNonInterestingEvents);
    }

    // get timecode string
    string tc = m.getTC();
    string textboxstring;

    // if there is a timecode, create the text box and overlay on the image 
    ostringstream os;
    if (tc.length() > 0) {
        os << tc << "/" << resultImg.getFrameNum();
    } else
        os << resultImg.getFrameNum();

    textboxstring =  os.str();

    // create a text box scaled from 720x480
    Image< PixRGB<byte> > textImg;
    const Dims d = resultImg.getDims();
    const int numW = (10 * d.w()) / 720;
    const int numH = (25 * d.h()) / 480;
    const int fntH = (20 * d.h()) / 480;

    // create the timecode text
    textImg.resize(numW * textboxstring.length(), numH, NO_INIT);
    textImg.clear(COL_WHITE);

    // set the maximum font height. This may not necessarily
    // be the maximum height, but will match the largest
    // that is closest to fntH
    const SimpleFont f = SimpleFont::fixedMaxHeight(fntH);

    writeText(textImg, Point2D<int>(0, 0), textboxstring.c_str(), COL_BLACK,
            COL_WHITE, f, true);

    // overlay the timecodeImg onto the final image
    Point2D<int> low_right(final_image.getWidth() - textImg.getWidth(),
            final_image.getHeight() - textImg.getHeight());

    pasteImage(final_image, textImg, COL_TRANSPARENT, low_right, itsOpacity.getVal());


    // display the frame?
    if (itsDisplayOutput.getVal()) {
        // make the label
        char label[2048];
        sprintf(label, "results%06d", resultImg.getFrameNum());
        resFrameWindow = displayImage(final_image, resFrameWindow, label);
    }
    // save the resulting frame to disk ?
    if (itsSaveOutput.getVal())
        itsOfs->writeFrame(GenericFrame(final_image), "results", FrameInfo("results", SRC_POS));
}

// #############################################################################

bool MbariResultViewer::isLoadEventsNameSet() const {
    return (itsLoadEventsName.getVal().length() > 0);
}


// #############################################################################

void MbariResultViewer::loadVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const {
    std::ifstream ifs(itsLoadEventsName.getVal().c_str());
    ves.readFromStream(ifs);
    ifs.close();
}
// #############################################################################

bool MbariResultViewer::isLoadPropertiesNameSet() const {
    return (itsLoadPropertiesName.getVal().length() > 0);
}

// #############################################################################

void MbariResultViewer::loadProperties(MbariVisualEvent::PropertyVectorSet& pvs) const {
    std::ifstream ifs(itsLoadPropertiesName.getVal().c_str());
    pvs.readFromStream(ifs);
    ifs.close();
}

// #############################################################################

bool MbariResultViewer::isSaveEventsNameSet() const {
    return (itsSaveEventsName.getVal().length() > 0);
}

// #############################################################################

void MbariResultViewer::saveVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const {
    std::ofstream ofs(itsSaveEventsName.getVal().c_str());
    ves.writeToStream(ofs);
    ofs.close();
}

// #############################################################################

bool MbariResultViewer::isSavePropertiesNameSet() const {
    return (itsSavePropertiesName.getVal().length() > 0);
}

// #############################################################################

void MbariResultViewer::saveProperties(MbariVisualEvent::PropertyVectorSet& pvs) {
    std::ofstream ofs;

    if (!appendProperties) { // if file hasn't been opened for appending, open to rewrite file
        ofs.open(itsSavePropertiesName.getVal().data());
        pvs.writeHeaderToStream(ofs);
        appendProperties = true;
    } else //otherwise for appending events
        ofs.open(itsSavePropertiesName.getVal().data(), std::ofstream::out | std::ofstream::app);

    pvs.writeToStream(ofs);
    ofs.close();
}

// #############################################################################

bool MbariResultViewer::isSavePositionsNameSet() const {
    return (itsSavePositionsName.getVal().length() > 0);
}

// #############################################################################

void MbariResultViewer::savePositions(const MbariVisualEvent::VisualEventSet& ves) const {
    std::ofstream ofs(itsSavePositionsName.getVal().c_str());
    ves.writePositions(ofs);
    ofs.close();
}

// #############################################################################

bool MbariResultViewer::needFrames() const {
    bool needOutput = itsSaveOutput.getVal() || itsDisplayOutput.getVal();
    bool needInput = !isLoadEventsNameSet() || isSaveEventClip();
    return (needInput || needOutput);
}

// #############################################################################

uint MbariResultViewer::getAvgCacheSize() const {
    return itsSizeAvgCache.getVal();
}

// #############################################################################

bool MbariResultViewer::isSaveEventClip() const {
    return (itsSaveEventNums.size() > 0);
}

// #############################################################################

uint MbariResultViewer::numSaveEventClips() const {
    return itsSaveEventNums.size();
}

// #############################################################################

uint MbariResultViewer::getSaveEventClipNum(uint idx) const {
    ASSERT(idx < itsSaveEventNums.size());
    return itsSaveEventNums[idx];
}

// #############################################################################

uint MbariResultViewer::getNumFromString(const string& resultName) {
    // see if we can find this guy in our list
    for (uint i = 0; i < itsResultNames.size(); ++i)
        if (itsResultNames[i].compare(resultName) == 0)
            return i;

    // didn't find it -> make a new entry and return index of this new entry
    itsResultNames.push_back(resultName);
    itsResultWindows.push_back(NULL);

    return (itsResultNames.size() - 1);
}

// #############################################################################

string MbariResultViewer::getLabel(const uint num, const uint frameNum,
        const int resNum) {
    ASSERT(num < itsResultNames.size());
    char fnum[7];
    sprintf(fnum, "%06d", frameNum);
    return (getFileStem(itsResultNames[num], resNum) + string(fnum));
}

// #############################################################################

string MbariResultViewer::getFileStem(const string& resultName,
        const int resNum) {
    return sformat("%s%02d_", resultName.c_str(), resNum);
}

// #############################################################################

template <class T>
XWinManaged* MbariResultViewer::displayImage(const Image<T>& img,
XWinManaged* win,
const char* label) {
    // need to rescale?
    Dims dims = itsRescaleDisplay.getVal();
    if (dims.isEmpty()) dims = img.getDims();
    bool doRescale = (dims != img.getDims());

    // does the window have to be re-constructed?
    if (win != NULL) {
        if (win->getDims() != dims) delete win;
    }

    if (win == NULL) {
        if (doRescale)
            win = new XWinManaged(rescale(img, dims), label);
        else
            win = new XWinManaged(img, label);
    } else {
        if (doRescale)
            win->drawImage(rescale(img, dims));
        else
            win->drawImage(img);

        win->setTitle(label);
    }

    return win;
}

// #############################################################################

void MbariResultViewer::saveSingleEventFrame(MbariImage< PixRGB<byte> >& img,
        int frameNum,
        MbariVisualEvent::VisualEvent *event) {
    ASSERT(event->frameInRange(frameNum));

    // create the file stem
    string evnum(sformat("evt%04d_", event->getEventNum()));

    const int pad = 10;
    Dims maxDims = event->getMaxObjectDims();
    Dims d(maxDims.w() + 2 * pad, maxDims.h() + 2 * pad);

    // compute the correct bounding box and cut it out
    Rectangle bbox = event->getToken(frameNum).bitObject.getBoundingBox();
    //Point2D cen = event.getToken(frameNum).bitObject.getCentroid();

    // first the horizontal direction
    int wpad = (d.w() - bbox.width()) / 2;
    int ll = bbox.left() - wpad;
    //int ll = cen.i - d.w() / 2;
    int rr = ll + d.w();
    if (ll < 0) {
        rr -= ll;
        ll = 0;
    }
    if (rr >= img.getWidth()) {
        rr = img.getWidth() - 1;
        ll = rr - d.w();
    }

    // now the same thing with the vertical direction
    int hpad = (d.h() - bbox.height()) / 2;
    int tt = bbox.top() - hpad;
    //int tt = cen.j - d.h() / 2;
    int bb = tt + d.h();
    if (tt < 0) {
        bb -= tt;
        tt = 0;
    }
    if (bb >= img.getHeight()) {
        bb = img.getHeight() - 1;
        tt = bb - d.h();
    }

    // cut out the rectangle and save it
    Image< PixRGB<byte> > cut = crop(img, Rectangle::tlbrI(tt, ll, bb, rr));
    itsEvtOfs->writeFrame(GenericFrame(cut), evnum, FrameInfo(evnum, SRC_POS));
}


// #############################################################################

void MbariResultViewer::saveVisualEvent(MbariVisualEvent::VisualEventSet& ves,
        std::list<MbariVisualEvent::VisualEvent *> &eventList) {
    std::ofstream ofs;

    if (!appendEvents) { // if file hasn't been opened for appending, open and write header
        ofs.open(itsSaveEventsName.getVal().data());
        ves.writeHeaderToStream(ofs);
        appendEvents = true;
    } else //otherwise write to append events and skip header
        ofs.open(itsSaveEventsName.getVal().data(), std::ofstream::out | std::ofstream::app);

    std::list<MbariVisualEvent::VisualEvent *>::iterator i;
    for (i = eventList.begin(); i != eventList.end(); ++i)
        (*i)->writeToStream(ofs);

    ofs.close();
}

// #############################################################################

bool MbariResultViewer::isSaveEventSummaryNameSet() const {
    return (itsSaveSummaryEventsName.getVal().length() > 0);
}

// #############################################################################

void MbariResultViewer::saveVisualEventSummary(string versionString,
        std::list<MbariVisualEvent::VisualEvent *> &eventList) {
    std::ofstream ofs;

    if (!appendEventSummary) { // if file hasn't been opened for appending, open and write header
        ofs.open(itsSaveSummaryEventsName.getVal().data());
        ofs << versionString;
        ofs << "filename:" << itsSaveSummaryEventsName.getVal();

        char datestamp[24];
        time_t time_of_day;
        time_of_day = time(NULL);
        strftime(datestamp, 24, "%Y-%m-%d %X %Z", localtime(&time_of_day));
        ofs << "\tcreated: ";
        ofs << datestamp << "\n";

        DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
        p.writeToStream(ofs);

        ofs << "eventID" << "\t";
        ofs << "startTimecode" << "\t";
        ofs << "endTimecode" << "\t";
        ofs << "startFrame" << "\t";
        ofs << "endFrame" << "\t";
        ofs << "startXY" << "\t";
        ofs << "endXY" << "\t";
        ofs << "maxArea" << "\t";
        ofs << "isInteresting" << "\n";
        appendEventSummary = true;
    } else //otherwise write to append events and skip header
        ofs.open(itsSaveSummaryEventsName.getVal().data(), std::ofstream::out | std::ofstream::app);

    MbariVisualEvent::Token tks, tke;
    uint sframe, eframe;
    Point2D<int> p;
    string tc;
    std::list<MbariVisualEvent::VisualEvent *>::iterator i;

    for (i = eventList.begin(); i != eventList.end(); ++i) {

        // if this is an interesting event, or
        // if this is a non-interesting(boring) event and we are saving boring events
        if ( (*i)->getCategory() == MbariVisualEvent::VisualEvent::INTERESTING ||
           ( (*i)->getCategory() == MbariVisualEvent::VisualEvent::BORING &&
                itsSaveNonInterestingEvents ) ) {
            ofs << (*i)->getEventNum() << "\t";

            if ((*i)->getStartTimecode().length() > 0)
                ofs << (*i)->getStartTimecode();
            else
                ofs << '-';
            ofs << "\t";

            if ((*i)->getEndTimecode().length() > 0)
                ofs << (*i)->getEndTimecode();
            else
                ofs << '-';
            ofs << "\t";

            sframe = (*i)->getStartFrame();
            eframe = (*i)->getEndFrame();
            tks = (*i)->getToken(sframe);
            tke = (*i)->getToken(eframe);

            ofs << sframe << "\t";
            ofs << eframe << "\t";
            p = tks.bitObject.getCentroid();
            ofs << p.i << "," << p.j;
            ofs << "\t";
            p = tke.bitObject.getCentroid();
            ofs << p.i << "," << p.j;
            ofs << "\t";
            ofs << (*i)->getMaxSize() << "\t";
            ofs << (*i)->getCategory() << "\t";
            ofs << "\n";
        }
    }

    ofs.close();
}

// #############################################################################

void MbariResultViewer::savePositions(const std::list<MbariVisualEvent::VisualEvent *> &eventList) const {

    std::ofstream ofs(itsSavePositionsName.getVal().data());

    std::list<MbariVisualEvent::VisualEvent *>::const_iterator i;
    for (i = eventList.begin(); i != eventList.end(); ++i)
        (*i)->writePositions(ofs);

    ofs.close();
}

// #############################################################################

bool MbariResultViewer::isSaveXMLEventsNameSet() const {
    return (itsSaveXMLEventSetName.getVal().length() > 0);
}

// #############################################################################

bool MbariResultViewer::hasMetadataSource() const {
    return (itsMetadataSource.getVal().length() > 0);
}

// #############################################################################

bool MbariResultViewer::isSaveAllEventClips() const {
    return itsSaveEventNumsAll;
}

// #############################################################################
// # Redifine the MbariResultViewer::parseSaveEventNums

void MbariResultViewer::parseSaveEventNums(const string& value) {
    itsSaveEventNums.clear();

    if (value.compare(string("all")) == 0) {
        itsSaveEventNumsAll = true;
    } else {
        // format here is "c,...,c"
        int curpos = 0, len = value.length();
        while (curpos < len) {
            // get end of next number
            int nextpos = value.find_first_not_of("-.0123456789eE", curpos);
            if (nextpos == -1) nextpos = len;

            // no number characters found -> bummer
            if (nextpos == curpos)
                LFATAL("Error parsing the SaveEventNum string '%s' - found '%c' "
                    "instead of a number.", value.data(), value[curpos]);

            // now let's see - can we get a number here?
            uint evNum;
            int rep = sscanf(value.substr(curpos, nextpos - curpos).data(), "%i", &evNum);

            // couldn't read a number -> bummer
            if (rep != 1)
                LFATAL("Error parsing SaveEventNum string '%s' - found '%s' instead of "
                    "a number.", value.data(),
                    value.substr(curpos, nextpos - curpos).data());

            // yeah! found a number -> store it
            itsSaveEventNums.push_back(evNum);

            LDEBUG("evNum = %i; value[nextpos] = '%c'", evNum, value[nextpos]);

            // not a comma -> bummer
            if ((nextpos < len) && (value[nextpos] != ','))
                LFATAL("Error parsing the SaveEventNum string '%s' - found '%c' "
                    "instead of ','.", value.data(), value[nextpos]);

            // the character right after the comma should be a number again
            curpos = nextpos + 1;
        }

        // end of string, done
    }
    return;
}
// #############################################################################

void MbariResultViewer::createXMLDocument(string versionString,
        FrameRange fr,
        string timecodefirst,
        string timecodelast,
        DetectionParameters params) {
    if (!XMLfileCreated) {
        itsXMLParser->creatDOMDocument(versionString,
                fr.getFirst(), fr.getLast(),
                timecodefirst, timecodelast);

        // add in source metadata if specified
        if (hasMetadataSource()) {
            itsXMLParser->addSourceMetaData(itsMetadataSource.getVal());
        }
        // add in detection parameters                             
        itsXMLParser->addDetectionParameters(params);
        itsXMLParser->writeDocument(itsSaveXMLEventSetName.getVal().c_str());
        XMLfileCreated = true;
    }
}
// #############################################################################

void MbariResultViewer::saveVisualEventSetToXML(std::list<MbariVisualEvent::VisualEvent *> &eventList,
        int eventframe,
        string eventframetimecode,
        FrameRange fr) {
    if (!XMLfileCreated)
        LFATAL("Error: Create an XML document first with createXMLDocument()");
    else {
        itsXMLParser->add(itsSaveNonInterestingEvents, eventList, eventframe, eventframetimecode);
        itsXMLParser->writeDocument(itsSaveXMLEventSetName.getVal().c_str());
    }

    if (fr.getLast() == eventframe) {
        if (!itsXMLParser->isXMLValid(itsSaveXMLEventSetName.getVal().c_str()))
            LFATAL("Error: There is something wrong with the XML auto generated");
        else
            LINFO("The XML output is valid");
    }
}

// #############################################################################
// Instantiations
#define INSTANTIATE(T) \
template void MbariResultViewer::output(const Image< T >& img, \
                                        const uint frameNum, \
                                        const string& resultName, \
                                        const int resNum); \
template void MbariResultViewer::display(const Image< T >& img, \
                                         const uint frameNum, \
                                         const string& resultName, \
                                         const int resNum); \
template XWinManaged* MbariResultViewer::displayImage(const Image< T >& img, \
                                                      XWinManaged* win, \
                                                      const char* label);

INSTANTIATE(PixRGB<byte>);
INSTANTIATE(byte);
INSTANTIATE(float);

