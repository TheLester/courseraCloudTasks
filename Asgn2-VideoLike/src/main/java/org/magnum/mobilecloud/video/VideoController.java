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

package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * 
 * @author lester
 *
 */

@Controller
public class VideoController {
	@Autowired
	private VideoRepository videoRepo;
	private AtomicLong likeCounter = new AtomicLong(0L);

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> listVideos() {
		return Lists.newArrayList(videoRepo.findAll());
	}

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video/{id}", method = RequestMethod.GET)
	public @ResponseBody Video getVideo(@PathVariable("id") Long videoID,
			HttpServletResponse response) throws IOException {
		Video requestedVideo = videoRepo.findOne(videoID);
		if (requestedVideo != null) {
			return requestedVideo;
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
	}

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		return videoRepo.save(video);
	}

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video/{id}/like", method = RequestMethod.POST)
	public void likeVideo(@PathVariable("id") long videoID, Principal user,
			HttpServletResponse response) throws IOException {
		Video likedVideo = videoRepo.findOne(videoID);
		if (likedVideo == null)
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		else {
			List<String> usersWhoLikedVideo = likedVideo
					.getUsersWhoLikedVideo();
			if (!usersWhoLikedVideo.contains(user.getName())) {
				usersWhoLikedVideo.add(user.getName());
				likedVideo.setUsersWhoLikedVideo(usersWhoLikedVideo);
				likeCounter.set(likedVideo.getLikes());
				likedVideo.setLikes(likeCounter.incrementAndGet());
				videoRepo.save(likedVideo);
			} else
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video/{id}/unlike", method = RequestMethod.POST)
	public void unlikeVideo(@PathVariable("id") long videoID, Principal user,
			HttpServletResponse response) throws IOException {
		Video unlikedVideo = videoRepo.findOne(videoID);
		if (unlikedVideo == null)
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		else {
			likeCounter.set(unlikedVideo.getLikes());
			unlikedVideo.setLikes(likeCounter.decrementAndGet());
			List<String> usersWhoLikedVideo = unlikedVideo
					.getUsersWhoLikedVideo();
			usersWhoLikedVideo.remove(user.getName());
			unlikedVideo.setUsersWhoLikedVideo(usersWhoLikedVideo);
			videoRepo.save(unlikedVideo);
		}
	}

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value = "/video/{id}/likedby", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(
			@PathVariable("id") long videoID) {
		Video requestedVideo = videoRepo.findOne(videoID);
		return requestedVideo.getUsersWhoLikedVideo();
	}
}
