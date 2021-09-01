package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository ContactRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);

		// get the user using usernname(Email)

		User user = userRepository.getUserByUserName(userName);

		System.out.println("USER " + user);

		model.addAttribute("user", user);
	}

	// home Dashboard
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add-Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			user.getContacts().add(contact);
			contact.setUser(user);

			// processing and uploading image file
			if (file.isEmpty()) {
				// if the image file is empty
				System.out.println("File is empty...default image will be set");
				contact.setImage("default.png");
			} else {
				// upload the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image is uploaded successfully...");

			}

			this.userRepository.save(user);

			System.out.println("Data " + contact);
			System.out.println("Added to database...");

			// success message
			session.setAttribute("message", new Message("Your Contact is added successfuly !! Add more...", "success"));
		} catch (Exception e) {
			System.out.println("ERROR " + e.getMessage());
			e.printStackTrace();
			// error message
			session.setAttribute("message", new Message("Something went wrong !! Please try again...", "danger"));
		}
		return "normal/add_contact";
	}

	// show contacts handler
	// per page 5 contacts
	// current page 0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show-User-Contacts");
		// return contacts lists
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
//		List<Contact> contacts = user.getContacts();

		PageRequest pageable = PageRequest.of(page, 5, Sort.unsorted());

		Page<Contact> contacts = this.ContactRepository.findContactsByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// showing perticular contact details
	@GetMapping("/contact/{cId}")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("Contact id : " + cId);

		Optional<Contact> contactOptional = this.ContactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session,
			Principal principal) {
		Optional<Contact> contactOptional = this.ContactRepository.findById(cId);
		Contact contact = contactOptional.get();

		// contact.setUser(null);
		// this.ContactRepository.delete(contact);
		// remove image also

		// check...

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		user.getContacts().remove(contact);
		this.userRepository.save(user);

		System.out.println("Contact deleted successfuly...");
		session.setAttribute("message", new Message("Contact Deleted Successfuly !!!", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// open update contact form handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");

		Contact contact = this.ContactRepository.findById(cId).get();

		model.addAttribute("contact", contact);

		return "normal/update_form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateContact(@ModelAttribute Contact contact, Model model,
			@RequestParam("profileImage") MultipartFile file, HttpSession session, Principal principal) {

		System.out.println("Contact Name : " + contact.getName());

		try {
			// old contact detail
			Contact oldContactDetail = this.ContactRepository.findById(contact.getcId()).get();

			// image
			if (!file.isEmpty()) {
				// file work.....rewrite...delete and update photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();

				// update new pic
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldContactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);

			this.ContactRepository.save(contact);

			session.setAttribute("message", new Message("Your Contact is updated successfuly", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/contact/" + contact.getcId();
	}

	// your profile handler
	@GetMapping("/profile")
	public String profileHandler(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	// open update user form handler
	@PostMapping("/update-user/{id}")
	public String userUpdateForm(@PathVariable("id") Integer id, Model model) {
		model.addAttribute("title", "Update User");

		User user = this.userRepository.findById(id).get();

		model.addAttribute("user", user);

		return "normal/user_update_form";
	}

	// update user details handler
	@RequestMapping(value = "/user-update", method = RequestMethod.POST)
	public String updateUser(@ModelAttribute User user, Model model, @RequestParam("profileImage") MultipartFile file,
			HttpSession session, Principal principal) {

		System.out.println("User Name : " + user.getName());

		try {
			// old contact detail
			User oldUserDetail = this.userRepository.findById(user.getId()).get();

			// image
			if (!file.isEmpty()) {
				// file work.....rewrite...delete and update photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldUserDetail.getImageUrl());
				file1.delete();

				// update new pic
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				user.setImageUrl(file.getOriginalFilename());
			} else {
				user.setImageUrl(oldUserDetail.getImageUrl());
			}

			this.userRepository.save(user);

			session.setAttribute("message", new Message("Your details are updated successfuly", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/profile";
	}

	// open settings handler....
	@GetMapping("/settings")
	public String openSettings(Model model) {
		model.addAttribute("title", "Settings");
		return "normal/settings";
	}

	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, Principal principal,
			HttpSession session, @RequestParam("newPassword") String newPassword,
			@RequestParam("confirmPassword") String confirmPassword) {

		System.out.println("Old Password : " + oldPassword);
		System.out.println("New Password : " + newPassword);
		System.out.println("Confirmed Password : " + confirmPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println("Current Password : " + currentUser.getPassword());

		if (this.passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// change the password...
			if (newPassword.equals(confirmPassword)) {
				currentUser.setPassword(this.passwordEncoder.encode(newPassword));
				this.userRepository.save(currentUser);
				session.setAttribute("message", new Message("Your password is updated successfuly...", "success"));
			} else {
				session.setAttribute("message",
						new Message("Your new password does not matches, please enter correct password !!!", "danger"));
				return "redirect:/user/settings";
			}

		} else {
			// error
			session.setAttribute("message",
					new Message("Your old password does not matches, please enter correct password !!!", "danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/profile";
	}

}
