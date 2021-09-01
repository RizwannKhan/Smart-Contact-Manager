package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {

	Random random = new Random(10000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwwordEncoder;

	// email id form open handler
	@RequestMapping("/forgot")
	public String openEmailForm() {

		return "forgot_email_form";
	}

	// sending otp to user email...
	@PostMapping("/send-otp")
	public String senfOTP(@RequestParam("email") String email, HttpSession session) {

		System.out.println("Email : " + email);

		// generating otp of 5 digit

		int otp = random.nextInt(99999);

		System.out.println("OTP : " + otp);

		// code for sending otp to email...
		String subject = "OTP from SCM";
		String message = "" + "<div style='border: 1px solid #e2e2e2; padding: 20px'>" + "<h1>" + "OTP is " + "<b>"
				+ otp + "</b>" + "</h1>" + "</div>";
		String to = email;

		boolean flag = this.emailService.sendEmail(subject, message, to);

		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "varify_otp";
		} else {
			session.setAttribute("message", "Check your email id !!!");
			return "forgot_email_form";
		}

	}

	// verify otp
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {

		int myOtp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (myOtp == otp) {
			// password change form
			User user = this.userRepository.getUserByUserName(email);

			if (user == null) {
				// send error message
				session.setAttribute("message", "User does not exists with this email !!!");
				return "forgot_email_form";
			} else {
				return "password_change_form";
			}
		} else {
			session.setAttribute("message", "You have entered wrong otp !!!");
			return "varify_otp";
		}

	}

	// change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.passwwordEncoder.encode(newpassword));
		this.userRepository.save(user);

		session.setAttribute("message", "User does not exists with this email !!!");
		return "redirect:/signin?change=password changed successfully !!!";
	}

}
