/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 
 * @author lester
 *
 */

@Controller
public class VideoController {
	private static final AtomicLong currentId = new AtomicLong(0L);
	private final Map<Long, Video> mapVideos = new HashMap<Long, Video>();
	private final VideoStatus readyStatus = new VideoStatus(
			VideoStatus.VideoState.READY);

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> listVideos() {
		return mapVideos.values();
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public void getVideo(@PathVariable("id") Long videoID,
			HttpServletResponse response) throws IOException {

		Video requestedVideo = mapVideos.get(videoID);
		if (requestedVideo != null) {
			VideoFileManager videoFileManager = VideoFileManager.get();
			videoFileManager.copyVideoData(requestedVideo,
					response.getOutputStream());
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}

	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		checkAndSetId(video);
		video.setDataUrl(getDataUrl(video.getId()));
		mapVideos.put(video.getId(), video);
		return video;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus uploadVideo(
			@PathVariable("id") Long videoID,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) throws IOException {

		VideoFileManager videoFileManager = VideoFileManager.get();
		Video requestedVideo = mapVideos.get(videoID);
		if (requestedVideo != null) {
			InputStream videoInputStream = videoData.getInputStream();
			videoFileManager.saveVideoData(requestedVideo, videoInputStream);
		} else
			response.sendError(HttpServletResponse.SC_NOT_FOUND);

		return readyStatus;
	}

	private void checkAndSetId(Video video) {
		if (video.getId() == 0) {
			video.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
