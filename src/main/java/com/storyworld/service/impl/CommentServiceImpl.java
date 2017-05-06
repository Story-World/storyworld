package com.storyworld.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.storyworld.domain.elastic.CommentContent;
import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;
import com.storyworld.domain.json.StatusMessage;
import com.storyworld.domain.sql.Comment;
import com.storyworld.domain.sql.Story;
import com.storyworld.domain.sql.User;
import com.storyworld.repository.sql.StoryRepository;
import com.storyworld.repository.elastic.CommentContentRepository;
import com.storyworld.repository.sql.CommentRepository;
import com.storyworld.repository.sql.UserRepository;
import com.storyworld.service.CommentService;
import com.storyworld.service.JSONService;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private StoryRepository storyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommentContentRepository commentContentRepository;

	@Autowired
	private JSONService jsonService;

	private static final Logger LOG = LoggerFactory.getLogger(CommentServiceImpl.class);

	@Override
	public void get(Long idStory, int page, int pageSize, Response response) {
		Story story = storyRepository.findOne(idStory);
		if (story != null && page > -1 && pageSize > 0) {
			Page<Comment> comments = commentRepository.findByStory(story,
					new PageRequest(page, pageSize, new Sort(Direction.DESC, "date")));
			List<CommentContent> commentsContent = new LinkedList<>();
			for (Comment comment : comments) {
				commentsContent.add(commentContentRepository.findOne(comment.get_id()));
				CommentContent commentContent = commentsContent.remove(commentsContent.size() - 1);
				commentContent.setDate(comment.getDate().toString());
				commentsContent.add(commentContent);
			}
			jsonService.prepareResponseForComment(response, null, null, commentsContent, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void save(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		Story story = storyRepository.findOne(request.getStory().getId());
		CommentContent commentContent = request.getCommentContent();
		Comment comment = commentRepository.findByAuthor(user);
		if (user != null && story != null && commentContent != null && comment == null) {
			comment = new Comment(user, story);
			try {
				user.setLastActionTime(LocalDateTime.now());
				userRepository.save(user);
				user.setRoles(null);
				user.setLastIncorrectLogin(null);
				user.setLastActionTime(null);
				user.setToken(null);
				user.setMail(null);
				commentContent.setAuthor(user);
				commentContent.setLikes(0);
				commentContent.setDislikes(0);
				commentContent = commentContentRepository.save(commentContent);
				comment.set_id(commentContent.getId());
				comment.setDate(LocalDateTime.now());
				commentRepository.save(comment);
				jsonService.prepareResponseForComment(response, StatusMessage.SUCCESS, "ADDED", null, commentContent,
						true);
			} catch (Exception e) {
				LOG.error(e.toString());
				jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
			}
		} else {
			if (comment != null)
				jsonService.prepareErrorResponse(response, "UNIQUE_COMMENT");
			else
				jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		}
	}

	@Override
	public void update(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		Comment comment = commentRepository.findBy_id(request.getCommentContent().getId());
		if (user != null && comment != null && request.getCommentContent() != null) {
			CommentContent commentContent = commentContentRepository.findOne(comment.get_id());
			commentContent.setEdited(true);
			commentContent.setContent(request.getCommentContent().getContent());
			comment.setDate(LocalDateTime.now());
			commentContent = commentContentRepository.save(commentContent);
			commentRepository.save(comment);
			user.setLastActionTime(LocalDateTime.now());
			userRepository.save(user);
			commentContent.setDate(LocalDateTime.now().toString());
			jsonService.prepareResponseForComment(response, StatusMessage.SUCCESS, "UPDATED", null, commentContent,
					true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void delete(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		Comment comment = commentRepository.findBy_id(request.getComment().get_id());
		if (comment != null) {
			CommentContent commentContent = commentContentRepository.findOne(comment.get_id());
			commentContentRepository.delete(commentContent);
			commentRepository.delete(comment);
			user.setLastActionTime(LocalDateTime.now());
			userRepository.save(user);
			jsonService.prepareResponseForComment(response, StatusMessage.SUCCESS, "DELETED", null, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public synchronized void like(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		CommentContent commentContent = commentContentRepository.findOne(request.getCommentContent().getId());
		if (user != null && commentContent != null) {
			int like = commentContent.getLikes();
			like++;
			commentContent.setLikes(like);
			commentContent = commentContentRepository.save(commentContent);
			user.setLastActionTime(LocalDateTime.now());
			userRepository.save(user);
			jsonService.prepareResponseForComment(response, StatusMessage.SUCCESS, "LIKED", null, commentContent, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public synchronized void dislike(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		CommentContent commentContent = commentContentRepository.findOne(request.getCommentContent().getId());
		if (commentContent != null) {
			int dislike = commentContent.getDislikes();
			dislike++;
			commentContent.setDislikes(dislike);
			commentContent = commentContentRepository.save(commentContent);
			user.setLastActionTime(LocalDateTime.now());
			userRepository.save(user);
			jsonService.prepareResponseForComment(response, StatusMessage.SUCCESS, "DISLIKED", null, commentContent,
					true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

}
