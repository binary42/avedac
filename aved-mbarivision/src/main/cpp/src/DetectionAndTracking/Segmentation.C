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

#ifdef HAVEMATLAB
#include "libJeromeSegmentAlgorithms.h"
#include "matrix.h"
#endif
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/segment/image.h"
#include "DetectionAndTracking/segment/misc.h"
#include "DetectionAndTracking/segment/pnmfile.h"
#include "DetectionAndTracking/segment/segment-image.h"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/MathOps.H"
#include "Raster/Raster.H"
#include "Raster/PngWriter.H"
#include <cstdio>
#include <cstdlib>

Segmentation::Segmentation() {
}

Segmentation::~Segmentation() {
}

// ######################################################################

Image<byte> Segmentation::runBinaryAdaptive(const Image<byte> &diff, const Image<byte> &bwImg, TrackingMode tm) {
    byte bgthreshold = getThreshold(diff, bwImg, tm);
    return (makeBinaryAdaptive(bwImg, bgthreshold, (const float) 0.5F));
}

// ######################################################################
Image< PixRGB<byte> > Segmentation::test(const Image < PixRGB<byte> >&input) {
  //printf("processing\n");
  int num_ccs;
  float sigma = 0.75;
  int k = 500;
  int min_size = 20;

  image<rgb> *im = new image<rgb>(input.getWidth(), input.getHeight());
  
    for (int x=0; x < input.getWidth(); x++)
    for (int y=0; y < input.getHeight(); y++) {
        rgb val;
        PixRGB<byte> val2 = input.getVal(x,y);
        val.r = val2.red();
        val.g = val2.green();
        val.b = val2.blue();
        imRef(im, x, y) = val;
    }

    // run segmentation
  image <rgb> *seg = segment_image(im, sigma, k, min_size, &num_ccs);

  // initialize the output image with the segmented results
  Image < PixRGB<byte> > output = input;
  for (int x=0; x < input.getWidth(); x++)
    for (int y=0; y < input.getHeight(); y++) {
        rgb val = imRef(seg, x, y);
        PixRGB<byte> val2((byte) val.r, (byte) val.g, (byte) val.b);
        output.setVal(x, y, val2) ;
  }
  
  delete im;
  delete seg;
  //printf("got %d components\n", num_ccs);
  //printf("done! uff...thats hard work.\n");
  return output;
}
// ###################################################################

/*
 * Segmentation function relying on a graph-cut
 *
 * @param: img = the current frame which has to be compared to the background
 * @param: backgroundMean =  It should be an image of the average background appearance, either from an empty frame or (better) the mean background 
 *                           over multiple empty frames.  If empty frames are unavailable, the median intensity may work as long as no space is occupied for 
 *                           half the time.
 */
Image<byte> Segmentation::runGraphCut(const Image< PixRGB<byte> >& image, const Image<PixRGB< byte> >& meanimg, const char *SEType) {
#ifdef HAVEMATLAB
    mxArray *segmentType;
    mxArray *inputfile;
    mxArray *background;

    if (SEType != NULL)
        LINFO("SEType %s unused for GraphCut algorithm and will be ignored", SEType);

    // write the background image
    background = mxCreateString("background.png");
    Raster::WriteRGB(meanimg, "background", RASFMT_PNG);

    // write the image to segment
    Raster::WriteRGB(image, "input", RASFMT_PNG);
    inputfile = mxCreateString("input.png");

    // run the algorithm and read back the results
    mlfExtractForegroundBW(inputfile, background);
    Image<byte> bitImg = Raster::ReadGray("input_segmented.ppm");
    return bitImg;
#else
    LFATAL("Matlab not installed - unable to run graphcut segmentation algorithm");
    exit(-1);
#endif
}

// ######################################################################

Image<byte> Segmentation::runAdaptiveThreshold(const Image<PixRGB <byte> > &img, const char *SEType) {
#ifdef HAVEMATLAB
    mxArray *segmentType;
    mxArray *inputfile;

    if (SEType == NULL)
        LFATAL("Invalid SEType");

    segmentType = mxCreateString(SEType);
    // write the input image
    Raster::WriteRGB(img, "input", RASFMT_PNG);
    inputfile = mxCreateString("input.png");

    mlfAdaptiveThresholding(inputfile, segmentType);
    Image<byte> bitImg = Raster::ReadGray("input_segmented.ppm");
    return bitImg;

#else
    LFATAL("Matlab not installed - unable to run graphcut segmentation algorithm");
    exit(-1);
#endif
}

// ######################################################################

Image<byte> Segmentation::runHomomorphicCanny(const Image< PixRGB<byte> > &img, const char *SEType) {
#ifdef HAVEMATLAB
    mxArray *segmentType;
    mxArray *inputfile;

    if (SEType == NULL)
        LFATAL("Invalid SEType");

    segmentType = mxCreateString(SEType);
    // write the input image
    Raster::WriteRGB(img, "input", RASFMT_PNG);
    inputfile = mxCreateString("input.png");

    mlfHomomorphicCanny(inputfile, segmentType);
    Image<byte> bitImg = Raster::ReadGray("input_segmented.ppm");
    return bitImg;
#else
    LFATAL("Matlab not installed - unable to run homomorphic canny segmentation algorithm");
    exit(-1);
#endif
}

// ######################################################################

Image<byte> Segmentation::runBackgroundCanny(const Image< PixRGB<byte> > &img, const char *SEType) {
#ifdef HAVEMATLAB
    mxArray *segmentType;
    mxArray *inputfile;

    if (SEType == NULL) 
        LFATAL("Invalid SEtype");

        segmentType = mxCreateString(SEType);

        // write the input image
        Raster::WriteRGB(img, "input", RASFMT_PNG);
        inputfile = mxCreateString("input.png");

        mlfBackgroundCanny(inputfile, segmentType);
        Image<byte> bitImg = Raster::ReadGray("input_segmented.ppm");
        return bitImg;
#else
    LFATAL("Matlab not installed - unable to run background canny segmentation algorithm");
    exit(-1);
#endif
    }

    // ######################################################################
    // ###### Private Fonctions related to the BinaryAdaptive algorithm #####
    // ######################################################################

    // ######################################################################

    byte Segmentation::getThreshold(const Image<byte> &diff, const Image<byte> &bwImg, TrackingMode tm) {
        byte bgthreshold = 5;
        double bwdiffmean = mean(diff);
        double bwdiffstddev = stdev(diff);
        float bwmean = mean(bwImg);
        float bwstddev = stdev(bwImg);

        if (abs(bwdiffstddev - bwdiffmean) < 2.F) {
            if (tm == TMKalmanFilter){
                bgthreshold = (byte) (4 * bwmean);
            }else{ 
                bgthreshold = (byte) (bwmean + 1.5 * bwstddev);
		}
        } else {
            bgthreshold = (byte) (bwmean + 1.5*bwstddev); 
	}

        return bgthreshold;
    }

    // ######################################################################

    Image<byte> Segmentation::makeBinaryAdaptive(const Image<byte>& src, const byte& threshold, const float& pct) {
        Image<byte> resultfinal(src.getDims(), ZEROS);
        Image<float> meanimg(src.getDims(), ZEROS);
        const int w = src.getWidth(), h = src.getHeight();
        float mymean, s, sum;
        int N, NC, col, inc;

        N = w*h;
        NC = w;
        s = (float) (NC * 2);
        col = 0;
        inc = 1;
        sum = 127.F * s;

        Image<byte>::const_iterator sptr = src.begin();
        Image<byte>::iterator rfinalptr = resultfinal.beginw();
        Image<float>::iterator meanptr = meanimg.beginw();

        for (int i = 0; i < N; i++) {
            if (col >= NC) {
                col = NC - 1;
                inc = -1;
                sptr += NC - 1;
                rfinalptr += NC - 1;
                meanptr += NC - 1;
            } else if (col < 0) {
                col = 0;
                inc = 1;
                sptr += NC;
                rfinalptr += NC;
                meanptr += NC;
            }
            sum = sum - sum / s + (float) * sptr;

            if (i > NC) {
                Image<float>::iterator ptr = meanptr;
                ptr -= NC;
                mymean = (sum + *ptr) / s;
            } else mymean = sum / s;

            *meanptr = mymean;
            int p = (int) (mymean * (100.0F - pct) / 100.0F);

      if (i > NC && *sptr > p && *sptr > threshold )
	*rfinalptr = 255; //white
      else
	*rfinalptr = 0; //black

      rfinalptr += inc;
      sptr += inc;
      col += inc;
      meanptr += inc;
    }
  return resultfinal;
}
