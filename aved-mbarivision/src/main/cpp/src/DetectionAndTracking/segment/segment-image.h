/*
Copyright (C) 2006 Pedro Felzenszwalb

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
*/

#ifndef SEGMENT_IMAGE
#define SEGMENT_IMAGE

#include <cstdlib>
#include "image.h"
#include "misc.h"
#include "filter.h"
#include "segment-graph.h"

// random color
rgb random_rgb(){ 
  rgb c;

  c.r = random();
  c.g = random();
  c.b = random();

  // exclude black since that's the mask color used in the image provided by --mbari-mask-path, e.g.
  while (c.r == 0 && c.g == 0 && c.b == 0) {
      c.r = random();
      c.g = random();
      c.b = random();
  }

  return c;
}

// dissimilarity measure between pixels
static inline float diff(image<float> *r, image<float> *g, image<float> *b,
			 int x1, int y1, int x2, int y2) {
  return sqrt(square(imRef(r, x1, y1)-imRef(r, x2, y2)) +
	      square(imRef(g, x1, y1)-imRef(g, x2, y2)) +
	      square(imRef(b, x1, y1)-imRef(b, x2, y2)));
}

/*
 * Segment an image
 *
 * Returns a color image representing the segmentation.
 *
 * im: image to segment.
 * sigma: to smooth the image.
 * c: constant for threshold function.
 * min_size: minimum component size (enforced by post-processing stage).
 * scaleW: amount to scale X seedWinner
 * scaleH: amount to scale H seedWinner.
 */
image<rgb> *segment_image(image<rgb> *im, float sigma, float c, int min_size, float scaleW, float scaleH) {
  int width = im->width();
  int height = im->height();

  image<float> *r = new image<float>(width, height);
  image<float> *g = new image<float>(width, height);
  image<float> *b = new image<float>(width, height);

  // smooth each color channel  
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      imRef(r, x, y) = imRef(im, x, y).r;
      imRef(g, x, y) = imRef(im, x, y).g;
      imRef(b, x, y) = imRef(im, x, y).b;
    }
  }
  image<float> *smooth_r = smooth(r, sigma);
  image<float> *smooth_g = smooth(g, sigma);
  image<float> *smooth_b = smooth(b, sigma);
  delete r;
  delete g;
  delete b;
 
  // build graph
  edge *edges = new edge[width*height*4];
  int num = 0;
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      if (x < width-1) {
	edges[num].a = y * width + x;
	edges[num].b = y * width + (x+1);
	edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x+1, y);
	num++;
      }

      if (y < height-1) {
	edges[num].a = y * width + x;
	edges[num].b = (y+1) * width + x;
	edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x, y+1);
	num++;
      }

      if ((x < width-1) && (y < height-1)) {
	edges[num].a = y * width + x;
	edges[num].b = (y+1) * width + (x+1);
	edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x+1, y+1);
	num++;
      }

      if ((x < width-1) && (y > 0)) {
	edges[num].a = y * width + x;
	edges[num].b = (y-1) * width + (x+1);
	edges[num].w = diff(smooth_r, smooth_g, smooth_b, x, y, x+1, y-1);
	num++;
      }
    }
  }
  delete smooth_r;
  delete smooth_g;
  delete smooth_b;

  // segment
  universe *u = segment_graph(width*height, num, edges, c);
  
  // post process small components
  for (int i = 0; i < num; i++) {
    int a = u->find(edges[i].a);
    int b = u->find(edges[i].b);
    if ((a != b) && ((u->size(a) < min_size) || (u->size(b) < min_size)))
      u->join(a, b);
  }
  delete [] edges;
  
  image<rgb> *output = new image<rgb>(width, height);

  // pick random colors for each component
  rgb *colors = new rgb[width*height];
  for (int i = 0; i < width*height; i++)
    colors[i] = random_rgb();

  bool found;
  rgb seedColor;
  std::list<rgb> seedColors;

  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      int comp = u->find(y * width + x);
      imRef(output, x, y) = colors[comp];
        seedColor = colors[comp];

            found = false;
            // add new colors to the list
            std::list<rgb>::const_iterator iter = seedColors.begin();
            while (iter != seedColors.end()) {
                rgb color = (*iter);
                if (color == seedColor) {
                    found = true;
                    break;
                }
            iter++;
            }
            if (found == false)
                seedColors.push_back(seedColor);
    }
  }

  /*Image< PixRGB<byte> > graphBitImg;

  for (int x = 0; x < output.getWidth(); x++)
    for (int y = 0; y < output.getHeight(); y++) {
        rgb val = imRef(output, x, y);
        PixRGB<byte> val2((byte) val.r, (byte) val.g, (byte) val.b);
        graphBitImg.setVal(x, y, val2);
    }

  Image<byte> labelImg(Dims(width*scaleW, height*scaleH), ZEROS);
  Image<byte> bitImg(Dims(width*scaleW, height*scaleH), ZEROS);

  // go through seed colors and create new bit objects for them
  std::list<rgb>::const_iterator iter = seedColors.begin();
  while (iter != seedColors.end()) {

        // create a binary representation with the 1 equal to the
        // color at the center of the seed everything else 0
        Image< PixRGB<byte> >::const_iterator sptr = graphBitImg.begin();
        Image<byte>::iterator rptr = bitImg.beginw();

        while (sptr != graphBitImg.end())
            *rptr++ = (*sptr++ == newColor) ? 1 : 0;

        // create bit object from that
        BitObject obj;
        Image<byte> dest = obj.reset(bitImg, Point2D<int>(rx, ry));
        WTAwinner win = WTAwinner::NONE();
        Point2D<int> center = obj.getCentroid();
        win.p.i = (int) ( (float) center.i*scaleW);
        win.p.j = (int) ( (float) center.j*scaleH);
        win.sv = 0.f;
        //LINFO("##### winner #%d found at [%d; %d]  frame: %d#####",
        //         numSpots, win.p.i, win.p.j, framenum);
        winners.push_back(Winner(win, obj));

        iter++;
  }*/

  delete [] colors;  
  delete u;

  return output;
}

#endif
