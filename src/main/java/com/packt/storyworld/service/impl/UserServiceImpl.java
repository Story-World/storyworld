package com.packt.storyworld.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.packt.storyworld.domain.json.Message;
import com.packt.storyworld.domain.json.Request;
import com.packt.storyworld.domain.json.Response;
import com.packt.storyworld.domain.json.StatusMessage;
import com.packt.storyworld.domain.sql.User;
import com.packt.storyworld.repository.sql.UserRepository;
import com.packt.storyworld.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public void removeToken(User user) {
		if (ChronoUnit.HOURS.between(user.getLastActionTime(), LocalDateTime.now()) >= 2) {
			user.setLastActionTime(null);
			user.setToken(null);
			userRepository.save(user);
		}
	}

	@Override
	public void login(Request request, Response response) {
		User userLogon = userRepository.findByName(request.getUser().getName());
		Message message = new Message();
		if (userLogon != null && userLogon.getName().equals(request.getUser().getName())
				&& userLogon.getPassword().equals(request.getUser().getPassword())) {
			response.setSuccess(true);
			message.setStatusMessage(StatusMessage.INFO);
			message.setMessage("LOGIN");
			response.setUser(userLogon);
			response.setMessage(message);
		} else {
			response.setSuccess(false);
			message.setStatusMessage(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void register(Request request, Response response) {
		if (request.getUser() != null) {
			User userRegister = userRepository.save(request.getUser());
			Message message = new Message();
			if (userRegister != null) {
				response.setSuccess(true);
				message.setStatusMessage(StatusMessage.INFO);
				message.setMessage("REGISTER");
				response.setMessage(message);
			} else {
				response.setSuccess(false);
				message.setStatusMessage(StatusMessage.ERROR);
				message.setMessage("INCORRECT_DATA");
				response.setMessage(message);
			}
		}
	}

}
