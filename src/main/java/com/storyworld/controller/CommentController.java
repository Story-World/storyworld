package com.storyworld.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;
import com.storyworld.service.AuthorizationService;
import com.storyworld.service.CommentService;

@RestController
@RequestMapping(value = "comment")
public class CommentController {

	@Autowired
	private CommentService commentService;

	@Autowired
	private AuthorizationService authorizationService;

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<Response> getCommetsByStory(@RequestBody Request request) {
		Response response = new Response();

		commentService.get(request, response);

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Response> saveCommet(@RequestBody Request request) {
		Response response = new Response();

		if (authorizationService.checkAccess(request))
			commentService.save(request, response);
		else
			return new ResponseEntity<Response>(response, HttpStatus.UNAUTHORIZED);

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.PUT)
	public ResponseEntity<Response> updateCommet(@RequestBody Request request) {
		Response response = new Response();

		if (authorizationService.checkAccessToComment(request))
			commentService.update(request, response);
		else
			return new ResponseEntity<Response>(response, HttpStatus.UNAUTHORIZED);

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Response> deleteCommet(@PathVariable(value = "id") Long id,
			@CookieValue("token") String token) {
		Response response = new Response();

		if (authorizationService.checkAccessToComment(new Request(token)))
			commentService.delete(id, response);
		else
			return new ResponseEntity<Response>(response, HttpStatus.UNAUTHORIZED);

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
}
