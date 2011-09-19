/*
 * @(#)MbarivisionOptions.java
 * 
 * Copyright 2011 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.mbari.aved.mbarivision.api;

import java.io.File;
import org.kohsuke.args4j.Option;

/** Defines options used in mbarivision - default value are set
 * 
 * When adding new options, make sure the associated member to each @Option is Public, e.g.
 * 		public MarkEventStyle markStyle;
 * 
 * If you want a default for the option, set it, e.g.
 * 		public MarkEventStyle markStyle = MarkEventStyle.BoundingBox;
 * 
 * Otherwise leave it blank and it will not be written to the XML file 
 * 		public MarkEventStyle markStyle;
 * */
public class MbarivisionOptions {

	/*
	 * List of all Mbarivision Options
	 **/
	public static final String MBARI_SEGMENTATION_ALGORITHM_OPTION = "--mbari-segment-algorithm";
	public enum SegmentationAlgorithm {BinaryAdaptive, Graphcut};
	@Option(name=MbarivisionOptions.MBARI_SEGMENTATION_ALGORITHM_OPTION,metaVar="BinaryAdaptive",required=false,usage="Sets the segmentation algorithm, valid options BinaryAdaptive|Graphcut")
	public SegmentationAlgorithm segmentAlgorithm = SegmentationAlgorithm.BinaryAdaptive;

	public static final String MBARI_MARK_INTERESTING_OPTION = "--mbari-mark-interesting";
	public enum MarkEventStyle {BoundingBox, Shape, Outline, None};
	@Option(name=MbarivisionOptions.MBARI_MARK_INTERESTING_OPTION,metaVar="BoundingBox",required=false,usage="Sets the way to outline the interesting event in the output frames, valid options BoundingBox, Shape, Outline, None")
	public MarkEventStyle eventStyle = MarkEventStyle.BoundingBox;

	public static final String MBARI_TRACKING_MODE_OPTION = "--mbari-tracking-mode";
	public enum TrackingMode {KalmanFilter,BoundingBox};
	@Option(name=MbarivisionOptions.MBARI_TRACKING_MODE_OPTION,metaVar="KalmanFilter",required=false,usage="Defines the tracking algorithm to track events, valid options KalmanFilter, BoundingBox")
	public TrackingMode trackingMode = TrackingMode.KalmanFilter;
	
	public static final String MBARI_SAVE_EVENT_CLIP_OPTION = "--mbari-save-event-clip";
	@Option(name=MbarivisionOptions.MBARI_SAVE_EVENT_CLIP_OPTION,metaVar="",required=false,usage="Saves all events as individual frames. This will crop out all the " +
			"events and beware - this generates many extra files, valid options all, evt1,evt2, etc.")
	public String saveEventClip;
	
	public static final String MBARI_SAVE_EVENT_SUMMARY_OPTION = "--mbari-save-event-summary";
	@Option(name=MbarivisionOptions.MBARI_SAVE_EVENT_SUMMARY_OPTION,metaVar="events.summary",required=false,usage="Saves human readable summary of the events to a text file")
	public File eventSummary = new File("events.summary");
		
	public static final String MBARI_SAVE_EVENTS_XML_OPTION = "--mbari-save-events-xml";
	@Option(name=MbarivisionOptions.MBARI_SAVE_EVENTS_XML_OPTION,metaVar="events.xml",required=false,usage="Save events data into XML format. " +
							"This saves more information than the event summary")
	public File eventxml = new File("events.xml");

	public static final String MBARI_SAVE_ONLY_INTERESTNIG_EVENTS_OPTION = "--mbari-save-only-interesting-events";
	@Option(name=MbarivisionOptions.MBARI_SAVE_ONLY_INTERESTNIG_EVENTS_OPTION,required=false,usage="Discard non-interesting events during processing")
	public Boolean saveOnlyInteresting = true;
	
	public static final String MBARI_NO_MARK_CANDIDATE_OPTION = "--nombari-mark-candidate";
	@Option(name=MbarivisionOptions.MBARI_NO_MARK_CANDIDATE_OPTION,required=false,usage="Diables marking of candidates for interesting events in output frames")
	public Boolean noMarkCandidate;
	
	public static final String MBARI_NO_LABEL_EVENTS_OPTION = "--nombari-label-events";
	@Option(name=MbarivisionOptions.MBARI_NO_LABEL_EVENTS_OPTION,required=false,usage="Diables marking event labels of interesting events in output frames")
	public Boolean noLabelEvents;
	
	public static final String MBARI_NO_SAVE_OUTPUT_OPTION = "--nombari-save-output";
	@Option(name=MbarivisionOptions.MBARI_NO_SAVE_OUTPUT_OPTION,required=false,usage="Diables saving output frames. If you set this, be sure you set --mbari-save-events-xml, " +
			"--mbari-save-event-summary, or --mbari-save-event-clip or you will have no recording of your events detection")
	public Boolean noSaveOutput;
	
	public static final String MBARI_CACHE_SIZE_OPTION = "--mbari-cache-size";
	@Option(name=MbarivisionOptions.MBARI_CACHE_SIZE_OPTION,required=false,usage="Number of frames to compute the running average. This is used in the background calculation")
	public int cacheSize = 30;		
	
	public static final String MBARI_MIN_EVENT_AREA_OPTION = "--mbari-min-event-area";
	@Option(name=MbarivisionOptions.MBARI_MIN_EVENT_AREA_OPTION,required=false,usage="Minimum area an event must be to be a candidate")
	public int minEventArea = 0;
	
	public static final String MBARI_MAX_EVENT_AREA_OPTION = "--mbari-max-event-area";
	@Option(name=MbarivisionOptions.MBARI_MAX_EVENT_AREA_OPTION,required=false,usage="Maximum area an event can be to be a candidate")
	public int maxEventArea = 10000; //TODO: this is arbitrary and should be based on the input image size [if known]
	
	public static final String MBARI_OPACITY_OPTION = "--mbari-opacity";
	@Option(name=MbarivisionOptions.MBARI_OPACITY_OPTION,required=false,usage="Defined opacity of marking of event in output frames, valid values 0.0-1.0")
	public double opacity = 1.0d;
	
	public static final String MBARI_MASK_FILE_OPTION = "--mbari-mask-file";
	@Option(name=MbarivisionOptions.MBARI_MASK_FILE_OPTION,required=false,usage="Define path to the image used to mask the detection. All events in the area defined by the white pixels will be ignored")
	public File maskFile;

	/**
	 * Clones the MbarivisionOptions object into an other one
	 */
	public MbarivisionOptions clone() {
		MbarivisionOptions d = new MbarivisionOptions();
		d.cacheSize = this.cacheSize;
		d.eventStyle = this.eventStyle;
		d.eventSummary = this.eventSummary;
		d.eventxml = this.eventxml;
		d.maskFile = this.maskFile;
		d.maxEventArea = this.maxEventArea;
		d.minEventArea = this.minEventArea;
		d.noLabelEvents = this.noLabelEvents;
		d.noMarkCandidate = this.noMarkCandidate;
		d.noSaveOutput = this.noSaveOutput;
		d.opacity = this.opacity;
		d.saveEventClip = this.saveEventClip;
		d.saveOnlyInteresting = this.saveOnlyInteresting;
		d.segmentAlgorithm = this.segmentAlgorithm;
		d.trackingMode = this.trackingMode;
		return d;
	}

	/**
	 * Dumps the information if the MbarivisionOptions onject
	 */
	public void dump() {
		System.out.println("MbarivisionOptions");
		System.out.println(MbarivisionOptions.MBARI_SEGMENTATION_ALGORITHM_OPTION + " : " + segmentAlgorithm);
		System.out.println(MbarivisionOptions.MBARI_TRACKING_MODE_OPTION + " : " + trackingMode);
		System.out.println(MbarivisionOptions.MBARI_MARK_INTERESTING_OPTION + " : " + eventStyle);
		System.out.println(MbarivisionOptions.MBARI_MASK_FILE_OPTION + " : " + maskFile);
		System.out.println(MbarivisionOptions.MBARI_OPACITY_OPTION + " : " + opacity);
		System.out.println(MbarivisionOptions.MBARI_MAX_EVENT_AREA_OPTION + " : " + maxEventArea);
		System.out.println(MbarivisionOptions.MBARI_MIN_EVENT_AREA_OPTION + " : " + minEventArea);
		System.out.println(MbarivisionOptions.MBARI_NO_SAVE_OUTPUT_OPTION + " : " + noSaveOutput);
		System.out.println(MbarivisionOptions.MBARI_SAVE_ONLY_INTERESTNIG_EVENTS_OPTION + " : " + saveOnlyInteresting);
		System.out.println(MbarivisionOptions.MBARI_SAVE_EVENT_SUMMARY_OPTION + " : " + eventSummary);
		System.out.println(MbarivisionOptions.MBARI_SAVE_EVENTS_XML_OPTION + " : " + eventxml);
		System.out.println(MbarivisionOptions.MBARI_SAVE_EVENT_CLIP_OPTION + " : " + saveEventClip);
	}
}
