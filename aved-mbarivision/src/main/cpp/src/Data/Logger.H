#ifndef LOGGER_C_DEFINED
#define LOGGER_C_DEFINED

#include "Image/OpenCVUtil.H"
#include "Component/ModelComponent.H"
#include "Component/ModelParam.H"
#include "Image/ArrayData.H"
#include "Image/Pixels.H"
#include "Features/HistogramOfGradients.H"
#include "Media/FrameRange.H"
#include "Media/FrameSeries.H"
#include "Util/Types.H"
#include "rutz/shared_ptr.h"

#include "Image/BitObjectDrawModes.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Image/MbariImage.H"
#include "Utils/MbariXMLParser.H"

#include <string>
#include <vector>

class ModelManager;
class MbariResultViewer;
template <class T> class MbariImage;

using namespace std;

// ######################################################################
//! Data logger class
class Logger : public ModelComponent
{

public:
    //! Construct
    Logger(OptionManager& mgr, nub::soft_ref<InputFrameSeries> ifs, nub::soft_ref<OutputFrameSeries> ofs,
        std::string logDir,
        const std::string& descrName = "mbarivision logger",
        const std::string& tagName = "logger");

    //! destructor
    virtual ~Logger();

    //! Overload so that we can reconfigure when our params get changed
    virtual void paramChanged(ModelParamBase* const param,
                            const bool valueChanged,
                            ParamClient::ChangeStatus* status);

    //! Logger
    void run(nub::soft_ref<MbariResultViewer> rv, MbariImage<PixRGB <byte> >& img,
             MbariVisualEvent::VisualEventSet& eventSet, const Dims scaledDims);

    //! save features from event clips
    void saveFeatures(int frameNum, MbariVisualEvent::VisualEventSet& eventSet, \
                        Image< PixRGB<byte> > &in, Image<byte> &prevmmap,
                        HistogramOfGradients &hog3x3, HistogramOfGradients &hog8x8,
                        MbariImage< PixRGB<byte> > &input,
                        MbariImage< PixRGB<byte> > &prevInput,
                        Dims scaledDims);

    //! Creates AVED XML document with header information:
    //! free memory
    virtual void reset1();

protected:

    //! overload start1()
    virtual void start1();

private:

    //! destroy internal variables
    void freeMem();


    //! save intermediate result
    /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of intermediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the result*/
    void save(const Image< PixRGB<byte> >& img, const uint frameNum,
        const string& resultName, const int resNum = -1);

    //! save intermediate result
    /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of intermediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
    void save(const Image<byte>& img, const uint frameNum,
        const string& resultName, const int resNum = -1);

    //! save intermediate result
    /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of intermediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
    void save(const Image<float>& img, const uint frameNum,
        const string& resultName, const int resNum = -1);

    //! save a cropped portion of the frame containing a single event
    void saveSingleEventFrame(const MbariImage< PixRGB<byte> >& img,
			    int frameNum, MbariVisualEvent::VisualEvent *event);

    //! save the PropertyVector to the file SavePropertiesName
    void saveProperties(MbariVisualEvent::PropertyVectorSet& pvs);

    //! load the VisualEventSet from the file LoadEventsName
    void loadVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const;

    //! load the PropertyVector from the file LoadPropertiesName
    void loadProperties(MbariVisualEvent::PropertyVectorSet& pvs) const;

    //! save the VisualEventSet to the file SaveEventsName
    void saveVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const;

    //! return the number of event clips we have to save
    uint numSaveEventClips() const;

    //! returns the event number with idx to be saved as a clip
    uint getSaveEventClipNum(uint idx) const;

    //! save a cut-out piece of frame containing a single event
    void saveSingleEventFrame(MbariImage< PixRGB<byte> >& img,
                int frameNum, MbariVisualEvent::VisualEvent *event);

    //! save the VisualEventList to the file SaveEventsName
    void saveVisualEvent(MbariVisualEvent::VisualEventSet& ves,
               std::list<MbariVisualEvent::VisualEvent *> &lves);

    //! save the positions to the file SavePositionsName
    void savePositions(const MbariVisualEvent::VisualEventSet& ves) const;

    /*! @param versionString the version string that represents this binary
    @param fr frame range of this event sequence
    @param timecodefirst starting timecode of this event sequence, e.g. in 00:01:12
    @param timecodelast ending timecode of this event sequence, e.g. in 00:01:12
    @param params detection parameters
    * */
    void createXMLDocument(string versionString,
             FrameRange fr,
             string timecodefirst,
             string timecodelast,
             DetectionParameters params);

    //! save the VisualEventList to the file saveXMLEventSetName
    /*! @param ves Visual Event Set
    @param currframe current frame we are writing
    @param timecode current frame timecode
    @param fr frame range for this event set
    */
    void saveVisualEventSetToXML(std::list<MbariVisualEvent::VisualEvent *> &ves,
                   int currframe,
                   string timecode,
                   FrameRange fr);

    //! save the VisualEventList to the file SaveEventsSummaryName
    void saveVisualEventSummary(string versionString,
                  std::list<MbariVisualEvent::VisualEvent *> &ves);

    //! save the positions to the file SavePositionsName
    void savePositions(const std::list<MbariVisualEvent::VisualEvent *> &ves) const;

    //! parse the SaveEventNumString and store the numbers in itsSaveEventNums
    void parseSaveEventNums(const string& value);

    //! make the file stem for saving files
    string getFileStem(const string& resultName, const int resNum = -1);

    //! check if save all event clips is set
    bool isSaveAllEventClips() const;

    OModelParam<FrameRange> itsInputFrameRange;
    OModelParam<string> itsInputFrameSource;
    OModelParam<string> itsOutputFrameSink;
    OModelParam<string> itsLoadEventsName;
    OModelParam<string> itsLoadPropertiesName;
    OModelParam<string> itsMetadataSource;
    OModelParam<bool> itsSaveBoringEvents; //! whether to save non-interesting/boring events
    OModelParam<string> itsSaveEventsName;
    OModelParam<string> itsSaveEventFeatures;
    OModelParam<string> itsSaveEventNumString;
    OModelParam<bool> itsSaveOriginalFrameSpec; //! True if saving output in the original (raw) frame specification
    OModelParam<bool> itsSaveOutput;      //! whether the output frames are saved
    OModelParam<string> itsSavePositionsName;
    OModelParam<string> itsSavePropertiesName;
    OModelParam<string> itsSaveSummaryEventsName;
    OModelParam<string> itsSaveXMLEventSetName;
    nub::soft_ref<InputFrameSeries> itsIfs;
    nub::soft_ref<OutputFrameSeries> itsOfs;

    MbariXMLParser* itsXMLParser;
    std::vector<uint> itsSaveEventNums;
    FrameRange itsFrameRange;
    bool itsXMLfileCreated;
    bool itsAppendEvt, itsAppendEvtSummary, itsAppendEvtXML, itsAppendProperties, itsSaveEventNumsAll;
    float itsScaleW, itsScaleH;
    Dims itsDims;
};

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

#endif // LOGGER_C_DEFINED
