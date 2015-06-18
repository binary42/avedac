#include "Image/OpenCVUtil.H"
#include "Data/Logger.H"

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
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Media/FrameSeries.H"
#include "Media/MbariResultViewer.H"
#include "Media/MediaOpts.H"
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

class ModelParamBase;
class DetectionParameters;

// ######################################################################
// Logger member definitions:
// ######################################################################

// ######################################################################
Logger::Logger(OptionManager& mgr, nub::soft_ref<InputFrameSeries> ifs,
      nub::soft_ref<OutputFrameSeries> ofs,
      std::string logDir,
      const std::string& descrName,
      const std::string& tagName)
      : ModelComponent(mgr, descrName, tagName),
    itsInputFrameRange(&OPT_InputFrameRange, this),
    itsInputFrameSource(&OPT_InputFrameSource, this),
    itsOutputFrameSink(&OPT_OutputFrameSink, this),
    itsLoadEventsName(&OPT_LOGloadEvents, this),
    itsLoadPropertiesName(&OPT_LOGloadProperties, this),
    itsMetadataSource(&OPT_LOGmetadataSource, this),
    itsSaveBoringEvents(&OPT_MDPsaveBoringEvents, this),
    itsSaveEventsName(&OPT_LOGsaveEvents, this),
    itsSaveEventFeatures(&OPT_LOGsaveEventFeatures, this),
    itsSaveEventNumString(&OPT_LOGsaveEventNums, this),
    itsSaveOriginalFrameSpec(&OPT_MDPsaveOriginalFrameSpec, this),
    itsSaveOutput(&OPT_LOGsaveOutput, this),
    itsSavePositionsName(&OPT_LOGsavePositions, this),
    itsSavePropertiesName(&OPT_LOGsaveProperties, this),
    itsSaveSummaryEventsName(&OPT_LOGsaveSummaryEventsName, this),
    itsSaveXMLEventSetName(&OPT_LOGsaveXMLEventSet, this),
    itsIfs(ifs),
    itsOfs(ofs),
    itsXMLfileCreated(false),
    itsAppendEvt(false),
    itsAppendEvtSummary(false),
    itsAppendEvtXML(false),
    itsAppendProperties(false),
    itsSaveEventNumsAll(false),
    itsScaleW(1.0f),
    itsScaleH(1.0f)
{
    itsXMLParser = new MbariXMLParser(logDir);
}

// ######################################################################
Logger::~Logger()
{
    freeMem();
}

// ######################################################################

void Logger::reset1() {
    // destroy our stuff
    freeMem();

    // propagate to our base class:
    ModelComponent::reset1();
}

// ######################################################################
void Logger::start1()
{
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

    // initialize the XML if requested to save event set to XML
    if (itsSaveOutput.getVal()) {
        Image< PixRGB<byte> > tmpimg;
        MbariImage< PixRGB<byte> > mstart(itsInputFrameSource.getVal());
        MbariImage< PixRGB<byte> > mend(itsInputFrameSource.getVal());

        // get the dimensions, starting and ending timecodes from the frames
        nub::ref<FrameIstream> rep = itsIfs->getFrameSource();

        rep->setFrameNumber(itsFrameRange.getFirst());
        tmpimg = rep->readRGB();
        mstart.updateData(tmpimg, itsFrameRange.getFirst());

        rep->setFrameNumber(itsFrameRange.getLast());
        tmpimg = rep->readRGB();
        mend.updateData(tmpimg, itsFrameRange.getLast());

        // create the XML document with header information
        createXMLDocument(Version::versionString(),
                itsFrameRange,
                mstart.getMetaData().getTC(),
                mend.getMetaData().getTC(),
                dp);

        // reset the frame number back since we will be streaming this soon
        rep->setFrameNumber((itsFrameRange.getFirst()));
    }

    //MbariVisualEvent::VisualEventSet eventSet;

    // are we loading the event structure from a file?
    if (itsLoadEventsName.getVal().length() > 0) {
        //loadVisualEventSet(eventSet);
        // TODO: test preloading of events; hasn't been used in a while but
        // potentially useful for future so will leave it here
    }
}

// ######################################################################
void Logger::paramChanged(ModelParamBase* const param,
                                 const bool valueChanged,
                                 ParamClient::ChangeStatus* status)
{
    // if the param is itsSaveEventNum, parse the string and fill the vector
    if (param == &itsSaveEventNumString)
        parseSaveEventNums(itsSaveEventNumString.getVal());

    if (param == &itsInputFrameRange)
        itsFrameRange = itsInputFrameRange.getVal();

}

// ######################################################################
void Logger::run(nub::soft_ref<MbariResultViewer> rv, MbariImage<PixRGB <byte> >& img,
                                        MbariVisualEvent::VisualEventSet& eventSet, const Dims scaledDims)
{
    // adjust scaling if needed
    Dims d = img.getDims();
    itsScaleW = (float)d.w()/(float)scaledDims.w();
    itsScaleH = (float)d.h()/(float)scaledDims.h();

    // initialize property vector and FOE estimator
    MbariVisualEvent::PropertyVectorSet pvs;

    // this is a list of all the events that have a token in this frame
    std::list<MbariVisualEvent::VisualEvent *> eventFrameList;

    // this is a complete list of all those events that are ready to be written
    std::list<MbariVisualEvent::VisualEvent *> eventListToSave;

    // get event frame list for this frame and those events that are ready to be saved
    // this is a list of all the events that have a token in this frame
    eventFrameList = eventSet.getEventsForFrame(img.getFrameNum());

    // this is a complete list of all those events that are ready to be written
    eventListToSave = eventSet.getEventsReadyToSave(img.getFrameNum());

    // write out eventSet?
    if (itsSaveEventsName.getVal().length() > 0 ) saveVisualEvent(eventSet, eventFrameList);

    // write out summary ?
    if (itsSaveSummaryEventsName.getVal().length() > 0) saveVisualEventSummary(Version::versionString(), eventListToSave);

    // flag events that have been saved for delete
    std::list<MbariVisualEvent::VisualEvent *>::iterator i;
    for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
        (*i)->flagWriteComplete();

    // write out positions?
    if (itsSavePositionsName.getVal().length() > 0) savePositions(eventFrameList);

    MbariVisualEvent::PropertyVectorSet pvsToSave = eventSet.getPropertyVectorSetToSave();

    // write out property vector set?
    if (itsSavePropertiesName.getVal().length() > 0) saveProperties(pvsToSave);

    // TODO: this is currently not used...look back in history to where this got cut-out
    // need to obtain the property vector set?
    if (itsLoadPropertiesName.getVal().length() > 0) pvs = eventSet.getPropertyVectorSet();

    // get a list of events for this frame
    eventFrameList = eventSet.getEventsForFrame(img.getFrameNum());

    // write out eventSet to XML?
    if (itsSaveXMLEventSetName.getVal().length() > 0) {
        saveVisualEventSetToXML(eventFrameList,
                img.getFrameNum(),
                img.getMetaData().getTC(),
                itsFrameRange);
    }

    const int circleRadiusRatio = 40;
    const int circleRadius = img.getDims().w() / circleRadiusRatio;

    Image< PixRGB<byte> > output = rv->createOutput(img,
            eventSet,
            circleRadius,
            itsScaleW, itsScaleH);

    // write  ?
    if (itsSaveOutput.getVal())
        itsOfs->writeFrame(GenericFrame(output), "results", FrameInfo("results", SRC_POS));

    // display output ?
    rv->display(output, img.getFrameNum(), "Results");

    // need to save any event clips?
    if (itsSaveEventNumsAll) {
        //save all events
        std::list<MbariVisualEvent::VisualEvent *>::iterator i;
        for (i = eventFrameList.begin(); i != eventFrameList.end(); ++i)
            saveSingleEventFrame(img, img.getFrameNum(), *i);
    } else {
        // need to save any particular event clips?
        uint csavenum = numSaveEventClips();
        for (uint idx = 0; idx < csavenum; ++idx) {
            uint evnum = getSaveEventClipNum(idx);
            if (!eventSet.doesEventExist(evnum)) continue;

            MbariVisualEvent::VisualEvent *event = eventSet.getEventByNumber(evnum);
            if (event->frameInRange(img.getFrameNum()))
                saveSingleEventFrame(img, img.getFrameNum(), event);
        }
    }

    //flag events that have been saved for delete otherwise takes too much memory
    for (i = eventListToSave.begin(); i != eventListToSave.end(); ++i)
        (*i)->flagForDelete();
    while (!eventFrameList.empty()) eventFrameList.pop_front();
    while (!eventListToSave.empty()) eventListToSave.pop_front();

}

// #############################################################################

void Logger::saveVisualEventSetToXML(std::list<MbariVisualEvent::VisualEvent *> &eventList,
        int eventframe,
        string eventframetimecode,
        FrameRange fr) {
    if (!itsXMLfileCreated)
        LFATAL("Error: Create an XML document first with createXMLDocument()");
    else {
		itsXMLParser->add(itsSaveBoringEvents.getVal(), eventList, eventframe, eventframetimecode, itsScaleW, itsScaleH);
	}

    if (fr.getLast() == eventframe) {
        if (!itsXMLParser->isXMLValid(itsSaveXMLEventSetName.getVal().c_str()))
            LFATAL("Error: There is something wrong with the XML auto generated");
        else {
            itsXMLParser->writeDocument(itsSaveXMLEventSetName.getVal().c_str());
            LINFO("The XML output is valid");
	}
    }
}

// #############################################################################

void Logger::createXMLDocument(string versionString,
        FrameRange fr,
        string timecodefirst,
        string timecodelast,
        DetectionParameters params) {
    if (!itsXMLfileCreated) {
        itsXMLParser->creatDOMDocument(versionString,
                fr.getFirst(), fr.getLast(),
                timecodefirst, timecodelast);

        // add in source metadata if specified
        if (itsMetadataSource.getVal().length() > 0) {
            itsXMLParser->addSourceMetaData(itsMetadataSource.getVal());
        }
        // add in detection parameters
        itsXMLParser->addDetectionParameters(params);
        itsXMLParser->writeDocument(itsSaveXMLEventSetName.getVal().c_str());
        itsXMLfileCreated = true;
    }
}

// #############################################################################

void Logger::savePositions(const std::list<MbariVisualEvent::VisualEvent *> &eventList) const {

    std::ofstream ofs(itsSavePositionsName.getVal().data());

    std::list<MbariVisualEvent::VisualEvent *>::const_iterator i;
    for (i = eventList.begin(); i != eventList.end(); ++i)
        (*i)->writePositions(ofs);

    ofs.close();
}


// #############################################################################
// # Redifine the Logger::parseSaveEventNums

void Logger::parseSaveEventNums(const string& value) {
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

void Logger::saveProperties(MbariVisualEvent::PropertyVectorSet& pvs) {
    std::ofstream ofs;

    if (!itsAppendProperties) { // if file hasn't been opened for appending, open to rewrite file
        ofs.open(itsSavePropertiesName.getVal().data());
        pvs.writeHeaderToStream(ofs);
        itsAppendProperties = true;
    } else //otherwise for appending events
        ofs.open(itsSavePropertiesName.getVal().data(), std::ofstream::out | std::ofstream::app);

    pvs.writeToStream(ofs);//TODO: test if need scaling factor here
    ofs.close();
}

// #############################################################################

void Logger::loadVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const {
    std::ifstream ifs(itsLoadEventsName.getVal().c_str());
    ves.readFromStream(ifs); //TODO: test if need scaling factor here
    ifs.close();
}

// ######################################################################

void Logger::freeMem() {

    itsSaveEventsName.setVal("");
    itsLoadEventsName.setVal("");
    itsSavePropertiesName.setVal("");
    itsLoadPropertiesName.setVal("");

    itsSaveSummaryEventsName.setVal("");
    itsLoadEventsName.setVal("");
    itsSaveXMLEventSetName.setVal("");

}

// #############################################################################

void Logger::saveFeatures(int frameNum, MbariVisualEvent::VisualEventSet& eventSet, \
                        Image< PixRGB<byte> > &in, Image<byte> &prevmmap,
                        HistogramOfGradients &hog3x3, HistogramOfGradients &hog8x8,
                        MbariImage< PixRGB<byte> > &input,
                        MbariImage< PixRGB<byte> > &prevInput,
                        Dims scaledDims) {

    if (itsSaveEventFeatures.getVal().length() > 0) {
        std::list<MbariVisualEvent::VisualEvent *> eventFrameList;
        eventFrameList = eventSet.getEventsForFrame(frameNum - 1);
        const std::string::size_type hashpos1 = itsOutputFrameSink.getVal().find_first_of(':');
        const std::string::size_type hashpos2 = itsOutputFrameSink.getVal().find_last_of('/');
        const std::string outputDir = itsOutputFrameSink.getVal().substr(hashpos1+1, hashpos2+1);

        // for each bit object, extract features and save the output
        std::list<MbariVisualEvent::VisualEvent *>::iterator event;
        for (event = eventFrameList.begin(); event != eventFrameList.end(); ++event) {

            LINFO("Getting features for event %d", (*event)->getEventNum());

            Rectangle bbox = (*event)->getToken(frameNum-1).bitObject.getBoundingBox();
            std::vector<double> featuresHOG3 = getFeaturesHOG(prevmmap, in, hog3x3, scaledDims, bbox);
            std::vector<double> featuresHOGMMAP3 = getFeaturesHOGMMAP(prevmmap, in, hog3x3, scaledDims, bbox);
            std::vector<double> featuresMBH3 = getFeaturesMBH(prevInput, input, hog3x3, scaledDims, bbox);
            std::vector<double> featuresHOG8 = getFeaturesHOG(prevmmap, in, hog8x8, scaledDims, bbox);
            std::vector<double> featuresHOGMAP8 = getFeaturesHOGMMAP(prevmmap, in, hog8x8, scaledDims, bbox);
            std::vector<double> featuresMBH8 = getFeaturesMBH(prevInput, input, hog8x8, scaledDims, bbox);

            // try to classify using histogram features
            //double prob = 0.;
            // int cls = bn.classify(features, &prob);

            // create the file stem and write out the features
            std::string evnumHOG3(sformat("%s%s_evt%04d_%06d_HOG_3.dat", outputDir.c_str(), itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1 ));
            std::string evnumHOGMMAP3(sformat("%s%s_evt%04d_%06d_HOGMMAP_3.dat", outputDir.c_str(),itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1 ));
            std::string evnumMBH3(sformat("%s%s_evt%04d_%06d_MBH_3.dat", outputDir.c_str(),itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1 ));
            std::string evnumHOG8(sformat("%s%s_evt%04d_%06d_HOG_8.dat", outputDir.c_str(),itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1 ));
            std::string evnumHOGMMAP8(sformat("%s%s_evt%04d_%06d_HOGMMAP_8.dat", outputDir.c_str(),itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1 ));
            std::string evnumMBH8(sformat("%s%s_evt%04d_%06d_MBH_8.dat", outputDir.c_str(),itsSaveEventFeatures.getVal().c_str(), (*event)->getEventNum(), frameNum-1));

            std::ofstream eofsHOG3(evnumHOG3.c_str());
            std::ofstream eofsHOGMMAP3(evnumHOGMMAP3.c_str());
            std::ofstream eofsMBH3(evnumMBH3.c_str());
            std::ofstream eofsHOG8(evnumHOG8.c_str());
            std::ofstream eofsHOGMMAP8(evnumHOGMMAP8.c_str());
            std::ofstream eofsMBH8(evnumMBH8.c_str());

            eofsHOG3.precision(12);
            eofsHOGMMAP3.precision(12);
            eofsMBH3.precision(12);
            eofsHOG8.precision(12);
            eofsHOGMMAP8.precision(12);
            eofsMBH8.precision(12);

            std::vector<double>::iterator eitrHOG3 = featuresHOG3.begin(), stopHOG3 = featuresHOG3.end();
            std::vector<double>::iterator eitrHOGMMAP3 = featuresHOGMMAP3.begin(), stopHOGMMAP3 = featuresHOGMMAP3.end();
            std::vector<double>::iterator eitrMBH3 = featuresMBH3.begin(), stopMBH3 = featuresMBH3.end();
            std::vector<double>::iterator eitrHOG8 = featuresHOG8.begin(), stopHOG8 = featuresHOG8.end();
            std::vector<double>::iterator eitrHOGMMAP8 = featuresHOGMAP8.begin(), stopHOGMMAP8 = featuresHOGMAP8.end();
            std::vector<double>::iterator eitrMBH8 = featuresMBH8.begin(), stopMBH8 = featuresMBH8.end();

            while(eitrHOG3 != stopHOG3)   eofsHOG3 << *eitrHOG3++ << " ";
            eofsHOG3.close();
            while(eitrHOGMMAP3 != stopHOGMMAP3)   eofsHOGMMAP3 << *eitrHOGMMAP3++ << " ";
            eofsHOGMMAP3.close();
            while(eitrMBH3 != stopMBH3)   eofsMBH3 << *eitrMBH3++ << " ";
            eofsMBH3.close();

            while(eitrHOG8 != stopHOG8)   eofsHOG8 << *eitrHOG8++ << " ";
            eofsHOG3.close();
            while(eitrHOGMMAP8 != stopHOGMMAP8)   eofsHOGMMAP8 << *eitrHOGMMAP8++ << " ";
            eofsHOGMMAP8.close();
            while(eitrMBH8 != stopMBH8)   eofsMBH8 << *eitrMBH8++ << " ";
            eofsMBH8.close();

            // if probability small, no matches found, so add a new class by event number
            //if (prob < 0.1) {
            //if ((*event)->getEventNum() <  maxClasses) {
            //    bn.learn(features,  (*event)->getEventNum());
            // }
        }
    }
}

// #############################################################################

void Logger::saveSingleEventFrame(MbariImage< PixRGB<byte> >& img,
        int frameNum,
        MbariVisualEvent::VisualEvent *event) {
    ASSERT(event->frameInRange(frameNum));

    // create the file stem
    string evnum;
    if (itsSaveEventFeatures.getVal().length() > 0)
        evnum = sformat("%s_evt%04d_", itsSaveEventFeatures.getVal().c_str(), event->getEventNum() );
    else
        evnum = sformat("evt%04d_", event->getEventNum());

    Dims maxDims = event->getMaxObjectDims();
    Dims d((float)maxDims.w()*itsScaleW, (float)maxDims.h()*itsScaleH);

    // compute the correct bounding box and cut it out
    Rectangle bbox1 = event->getToken(frameNum).bitObject.getBoundingBox();
    Rectangle bbox = Rectangle::tlbrI(bbox1.top()*itsScaleH, bbox1.left()*itsScaleW,
                                    bbox1.bottomI()*itsScaleH, bbox1.rightI()*itsScaleW);
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

    Rectangle bboxFinal = Rectangle::tlbrI(tt, ll, bb, rr);
    bboxFinal = bboxFinal.getOverlap(Rectangle(Point2D<int>(0, 0), img.getDims() - 1));

    // scale if needed and cut out the rectangle and save it
    Image< PixRGB<byte> > cut = crop(img, bboxFinal);
    itsOfs->writeFrame(GenericFrame(cut), evnum, FrameInfo(evnum, SRC_POS));
}


// #############################################################################

void Logger::saveVisualEvent(MbariVisualEvent::VisualEventSet& ves,
        std::list<MbariVisualEvent::VisualEvent *> &eventList) {
    std::ofstream ofs;

    if (!itsAppendEvt) { // if file hasn't been opened for appending, open and write header
        ofs.open(itsSaveEventsName.getVal().data());
        ves.writeHeaderToStream(ofs);
        itsAppendEvt = true;
    } else //otherwise write to append events and skip header
        ofs.open(itsSaveEventsName.getVal().data(), std::ofstream::out | std::ofstream::app);

    std::list<MbariVisualEvent::VisualEvent *>::iterator i;
    for (i = eventList.begin(); i != eventList.end(); ++i)
        (*i)->writeToStream(ofs);

    ofs.close();
}

// #############################################################################

void Logger::saveVisualEventSummary(string versionString,
        std::list<MbariVisualEvent::VisualEvent *> &eventList) {
    std::ofstream ofs;

    if (!itsAppendEvtSummary) { // if file hasn't been opened for appending, open and write header
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
        itsAppendEvtSummary = true;
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
                itsSaveBoringEvents.getVal() ) ) {
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

void Logger::save(const Image< PixRGB<byte> >& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void Logger::save(const Image<byte>& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void Logger::save(const Image<float>& img,
        const uint frameNum,
        const string& resultName,
        const int resNum) {
    itsOfs->writeFrame(GenericFrame(img, FLOAT_NORM_0_255), getFileStem(resultName, resNum), FrameInfo(resultName, SRC_POS));
}

// #############################################################################

void Logger::loadProperties(MbariVisualEvent::PropertyVectorSet& pvs) const {
    std::ifstream ifs(itsLoadPropertiesName.getVal().c_str());
    pvs.readFromStream(ifs);
    ifs.close();
}

// #############################################################################

void Logger::saveVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const {
    std::ofstream ofs(itsSaveEventsName.getVal().c_str());
    ves.writeToStream(ofs);
    ofs.close();
}

/// #############################################################################

uint Logger::numSaveEventClips() const {
    return itsSaveEventNums.size();
}
// #############################################################################

void Logger::savePositions(const MbariVisualEvent::VisualEventSet& ves) const {
    std::ofstream ofs(itsSavePositionsName.getVal().c_str());
    ves.writePositions(ofs);
    ofs.close();
}

// #############################################################################

uint Logger::getSaveEventClipNum(uint idx) const {
    ASSERT(idx < itsSaveEventNums.size());
    return itsSaveEventNums[idx];
}

// #############################################################################

string Logger::getFileStem(const string& resultName,
        const int resNum) {
    return sformat("%s%02d_", resultName.c_str(), resNum);
}

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
