package com.storyworld.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;
import com.storyworld.domain.json.StatusMessage;
import com.storyworld.domain.sql.Mail;
import com.storyworld.domain.sql.MailToken;
import com.storyworld.domain.sql.Role;
import com.storyworld.domain.sql.User;
import com.storyworld.enums.Status;
import com.storyworld.enums.TypeToken;
import com.storyworld.repository.sql.MailReposiotory;
import com.storyworld.repository.sql.MailTokenRepository;
import com.storyworld.repository.sql.RoleRepository;
import com.storyworld.repository.sql.UserRepository;
import com.storyworld.service.JSONService;
import com.storyworld.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private MailTokenRepository mailTokenRepository;

	@Autowired
	private MailReposiotory mailReposiotory;

	@Autowired
	private JSONService jsonService;

	private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

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

		if (userLogon != null && userLogon.getName().equals(request.getUser().getName())
				&& userLogon.getPassword().equals(request.getUser().getPassword())
				&& (!userLogon.isBlock() || !userLogon.isBlock() || (userLogon.getLastIncorrectLogin() != null
						&& ChronoUnit.MINUTES.between(userLogon.getLastIncorrectLogin(), LocalDateTime.now()) >= 10))) {
			userLogon.setToken(UUID.randomUUID().toString());
			userLogon.setLastActionTime(LocalDateTime.now());
			userLogon.setBlock(false);
			userLogon.setIncorrectLogin(0);
			userLogon.setLastIncorrectLogin(null);
			userRepository.save(userLogon);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "LOGIN", userLogon, null, true);
		} else {
			if (userLogon != null) {
				int incrementIncorrect = userLogon.getIncorrectLogin();
				incrementIncorrect++;
				userLogon.setIncorrectLogin(incrementIncorrect);
				if (userLogon.getIncorrectLogin() == 5) {
					userLogon.setBlock(true);
					userLogon.setLastIncorrectLogin(LocalDateTime.now());
				}
				userRepository.save(userLogon);
			}
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		}
	}

	@Override
	public void register(Request request, Response response) {
		try {
			User user = request.getUser();
			user.setBlock(true);
			user.setDelete(false);
			User userRegister = userRepository.save(user);
			Role role = roleRepository.findOne((long) 1);
			Set<Role> roles = new HashSet<>();
			roles.add(role);
			userRegister.setRoles(roles);
			userRepository.save(userRegister);
			MailToken mailToken = new MailToken(TypeToken.REGISTER, UUID.randomUUID().toString(), LocalDateTime.now(),
					userRegister);
			Set<MailToken> tokens = new HashSet<>();
			tokens.add(mailToken);
			mailTokenRepository.save(tokens);
			Mail mail = new Mail(TypeToken.REGISTER, Status.READY, userRegister);
			mailReposiotory.save(mail);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "REGISTER", null, null, true);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		}
	}

	@Override
	public void restartPassword(Request request, Response response) {
		User user = userRepository.findByMail(request.getUser().getMail());

		if (user != null) {
			MailToken mailToken = mailTokenRepository.findByUserAndTypeToken(user, TypeToken.RESTART_PASSWORD);
			if (mailToken == null) {
				mailToken = new MailToken(TypeToken.RESTART_PASSWORD, UUID.randomUUID().toString(), LocalDateTime.now(),
						user);
				Set<MailToken> tokens = mailTokenRepository.findByUser(user);
				tokens.add(mailToken);
				mailTokenRepository.save(tokens);
			} else {
				long id = mailToken.getId();
				Set<MailToken> tokens = mailTokenRepository.findByUser(user);
				tokens.removeIf(x -> x.getId() == id);
				mailToken.setValidationTime(LocalDateTime.now());
				mailToken.setToken(UUID.randomUUID().toString());
				tokens.add(mailToken);
				mailTokenRepository.save(tokens);
			}
			Mail mail = new Mail(TypeToken.RESTART_PASSWORD, Status.READY, user);
			mailReposiotory.save(mail);
			jsonService.prepareResponseForUser(response, StatusMessage.INFO, "RESTARTED", null, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void remindPassword(Request request, Response response) {
		MailToken mailToken = mailTokenRepository.findByToken(request.getToken());

		if (mailToken != null && mailToken.getTypeToken().equals(TypeToken.RESTART_PASSWORD)
				&& ChronoUnit.DAYS.between(mailToken.getValidationTime(), LocalDateTime.now()) <= 1
				&& mailToken.getToken().equals(request.getToken())) {
			User user = userRepository.findOne(mailToken.getUser().getId());
			try {
				user.setPassword(request.getUser().getPassword());
				userRepository.save(user);
				mailTokenRepository.delete(mailToken);
				jsonService.prepareResponseForUser(response, StatusMessage.INFO, "RESTARTED", null, null, true);
			} catch (Exception e) {
				LOG.error(e.getMessage());
				jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
			}
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void confirmRegister(Request request, Response response) {
		MailToken mailToken = mailTokenRepository.findByToken(request.getToken());

		if (mailToken != null && mailToken.getTypeToken().equals(TypeToken.REGISTER)
				&& mailToken.getToken().equals(request.getToken())) {
			User user = userRepository.findOne(mailToken.getUser().getId());
			user.setLastActionTime(LocalDateTime.now());
			user.setBlock(false);
			userRepository.save(user);
			mailTokenRepository.delete(mailToken);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "SUCCESS_REGISTERED", null, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void changePassword(Request request, Response response) {
		User user = userRepository.findOne(request.getUser().getId());

		try {
			user.setPassword(request.getUser().getPassword());
			userRepository.save(user);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "SUCCESS_UPDATED", user, null, true);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		}
	}

	@Override
	public void updateUser(Request request, Response response) {
		User user = userRepository.findOne(request.getUser().getId());

		try {
			if (request.getUser().getName() != null)
				user.setName(request.getUser().getName());
			if (request.getUser().getMail() != null)
				user.setMail(request.getUser().getMail());
			userRepository.save(user);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "SUCCESS_UPDATED", user, null, true);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		}
	}

	@Override
	public void getUser(Request request, Response response) {
		User user = null;

		if (request.getUser() != null)
			user = userRepository.findOne(request.getUser().getId());
		else
			user = userRepository.findByToken(request.getToken());

		if (user != null) {
			user.setToken(null);
			jsonService.prepareResponseForUser(response, null, null, user, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void logout(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		if (user != null) {
			user.setToken(null);
			user.setLastActionTime(null);
			userRepository.save(user);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "LOGOUT", null, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void getUsers(Request request, Response response) {
		if (request.getPage() > -1 && request.getSizePage() > 0) {
			Page<User> users = userRepository.findAll(new PageRequest(request.getPage(), request.getSizePage()));
			if (users != null)
				jsonService.prepareResponseForUser(response, null, null, null, users, true);
			else
				jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

	@Override
	public void delete(Long id, Response response) {
		User user = userRepository.findOne(id);
		if (user != null) {
			user.setDelete(true);
			userRepository.save(user);
			jsonService.prepareResponseForUser(response, StatusMessage.SUCCESS, "DELTED", user, null, true);
		} else
			jsonService.prepareErrorResponse(response, "INCORRECT_DATA");
	}

}