#include "DetectionAndTracking/ObjectDetection.H"

#include "Component/OptionManager.H"
#include "Component/ParamClient.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/ColorOps.H"
#include "Image/Image.H"
#include "Image/PixelsTypes.H"
#include "Util/StringConversions.H"

class ModelParamBase;
class DetectionParameters;

// ######################################################################
// ObjectDetection member definitions:
// ######################################################################

// ######################################################################
ObjectDetection::ObjectDetection(OptionManager& mgr,
      const std::string& descrName,
      const std::string& tagName)
      : ModelComponent(mgr, descrName, tagName)
{

}

// ######################################################################
ObjectDetection::~ObjectDetection()
{  }

// ######################################################################
void ObjectDetection::start1()
{

}

// ######################################################################
void ObjectDetection::paramChanged(ModelParamBase* const param,
                                 const bool valueChanged,
                                 ParamClient::ChangeStatus* status)
{

}

// ######################################################################
std::list<BitObject> ObjectDetection::run(nub::soft_ref<MbariResultViewer> rv,
    const std::list<Winner> &winlist,
    const Image< PixRGB<byte> > &segmentInImg)
{
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<BitObject> bosFiltered;
    std::list<BitObject> bosUnfiltered;
    std::list<Winner>::const_iterator iter = winlist.begin();

    //go through each winner and extract salient objects
    while (iter != winlist.end()) {

        // get the foa mask
        BitObject boFOA = (*iter).getBitObject();
        WTAwinner winner = (*iter).getWTAwinner();

        // if the foa mask area is too small, we aren't going to find any large enough objects so bail out
        if (boFOA.getArea() <  p.itsMinEventArea) {
            iter++;
            continue;
        }

        // if only using the foamask region and not the foamask to guide the detection
        if (p.itsUseFoaMaskRegion) {
            LINFO("----------------->Using FOA mask region");
            Rectangle foaregion = boFOA.getBoundingBox();
            Point2D<int> center = boFOA.getCentroid();
            Dims d = segmentInImg.getDims();
            Dims segmentDims = Dims((float)foaregion.width()*2.0,(float)foaregion.height()*2.0);
            Dims searchDims = Dims((float)foaregion.width(),(float)foaregion.height());
            Rectangle searchRegion = Rectangle::centerDims(center, searchDims);
            searchRegion = searchRegion.getOverlap(Rectangle(Point2D<int>(0, 0), segmentInImg.getDims() - 1));
            Rectangle segmentRegion = Rectangle::centerDims(center, segmentDims);
            segmentRegion = segmentRegion.getOverlap(Rectangle(Point2D<int>(0, 0), segmentInImg.getDims() - 1));

            // get the region used for searching for a match based on the foa region
            LINFO("Extracting bit objects from frame %d winning point %d %d/region %s minSize %d maxSize %d %d %d", \
                   (*iter).getFrameNum(), winner.p.i, winner.p.j, convertToString(searchRegion).c_str(), p.itsMinEventArea,
                    p.itsMaxEventArea, d.w(), d.h());

            std::list<BitObject> sobjs = extractBitObjects(segmentInImg, center, searchRegion, segmentRegion, (float)p.itsMinEventArea/2.F, p.itsMaxEventArea);
            std::list<BitObject> sobjsKeep;

            // need at least two objects to find a match, otherwise just background
            if (sobjs.size() > 1 ) {

                // set the winning voltage for each winning bit object
                std::list<BitObject>::iterator iter;
                for (iter = sobjs.begin(); iter != sobjs.end(); ++iter) {
                    if ( (*iter).getArea() >= p.itsMaxEventArea && (*iter).getArea() <= p.itsMaxEventArea) {
                        (*iter).setSMV(winner.sv);
                        sobjsKeep.push_back(*iter);
                    }
                }

                // add to the list
                bosUnfiltered.splice(bosUnfiltered.begin(), sobjsKeep);
            }
            else {
                LINFO("Can't find bit object, checking FOA mask");
                if (boFOA.getArea() >= p.itsMinEventArea && boFOA.getArea() <= p.itsMaxEventArea) {
                    boFOA.setSMV(winner.sv);
                    sobjsKeep.push_back(boFOA);
                    bosUnfiltered.splice(bosUnfiltered.begin(), sobjsKeep);
                    LINFO("FOA mask ok %d < %d < %d", p.itsMinEventArea, boFOA.getArea(), p.itsMaxEventArea);
                }
                else
                    LINFO("FOA mask too large %d > %d or %d > %d",boFOA.getArea(), p.itsMinEventArea,
                    boFOA.getArea(), p.itsMaxEventArea);
                    }
        }
        else {

            LINFO("----------------->Using FOA mask only");
            if (boFOA.getArea() >= p.itsMinEventArea && boFOA.getArea() <= p.itsMaxEventArea) {
                boFOA.setSMV(winner.sv);
                bosUnfiltered.push_back(boFOA);
                LINFO("FOA mask ok %d < %d < %d", p.itsMinEventArea, boFOA.getArea(), p.itsMaxEventArea);
            }
            else
                LINFO("FOA mask too large %d > %d or %d > %d",boFOA.getArea(), p.itsMinEventArea,
                boFOA.getArea(), p.itsMaxEventArea);
        }

        iter++;
    }// end while iter != winners.end()

    LINFO("Found %i bitobject(s)", bosUnfiltered.size());

    bool found;
    int minSize = p.itsMaxEventArea;

    // loop until we find all non-overlapping objects starting with the smallest
    while (!bosUnfiltered.empty()) {

        std::list<BitObject>::iterator biter, siter, smallest;
        // find the smallest object
        smallest = bosUnfiltered.begin();
        for (siter = bosUnfiltered.begin(); siter != bosUnfiltered.end(); ++siter)
            if (siter->getArea() < minSize) {
                minSize = siter->getArea();
                smallest = siter;
            }

        // does the smallest object intersect with any of the already stored ones
        found = true;
        for (biter = bosFiltered.begin(); biter != bosFiltered.end(); ++biter) {
            if (smallest->isValid() && biter->isValid() && biter->doesIntersect(*smallest)) {
                // no need to store intersecting objects -> get rid of smallest
                // and look for the next smallest
                bosUnfiltered.erase(smallest);
                found = false;
                break;
            }
        }

        if (found && smallest->isValid())
            bosFiltered.push_back(*smallest);
    }


    LINFO("Found total %d non intersecting objects", bosFiltered.size());
    return bosFiltered;
}

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
