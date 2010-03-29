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

#define UNSIGNED_TYPES
#include "Image/Image.H"
#include "Image/FilterOps.H"
#include "Raster/RasterFileFormat.H"
#include "Raster/Raster.H"
#include <stdio.h>
#include "mpi.h"
#include "mpe.h"
#include "mpe_log.h"
extern "C" {
#include <pipt.h>
}
#include <stdio.h>
#include <malloc.h>

int main( int argc, const char **argv )
{		
    int rank, size, num_workers;
	int max_workers = -1;
	Image< PixRGB<byte> > imgin;      
	Image< PixRGB<byte> > imgout;      	
	unsigned int height;
	unsigned int width;
	IMAGE *in, *out;


	printf("\n\nPIPT Sample driver program\n");
    	
	/* Initializing the PIPT master */
	printf("Initializing worker processes...\n\n");
	PIPT_Init(argc, (char **)argv); 

	if (rank == 0) MPE_Describe_state(1,2,"CachePreprocess", "purple:vlines3");

	
	PIPT_Get_numworkers(&num_workers, &max_workers);
	printf("\n%d workers initialized.\n\n", num_workers);
    
	if (rank > 0) {			 
		in= loadTiff("test-lowpass.tif");
		imgin = Raster::ReadRGB(Auto, "test-lowpass.ppm");
		width = imgin.getWidth();
		height = imgin.getHeight();
		MPE_Log_event(1,0,"");			
		out = IPAverage(in, height, width);		
		MPE_Log_event(2,0,"");		
		
	}

	printf("\n\nTelling workers to quit...\n");
	PIPT_Exit();
	printf("Workers have completed.\n\n\n");	
    return 0;
}

