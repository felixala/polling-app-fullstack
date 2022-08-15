package com.felixlaura.pollingapp.controller;

import com.felixlaura.pollingapp.exception.ResourceNotFoundException;
import com.felixlaura.pollingapp.model.User;
import com.felixlaura.pollingapp.payload.*;
import com.felixlaura.pollingapp.repository.PollRepository;
import com.felixlaura.pollingapp.repository.UserRepository;
import com.felixlaura.pollingapp.repository.VoteRepository;
import com.felixlaura.pollingapp.security.CurrentUser;
import com.felixlaura.pollingapp.security.UserPrincipal;
import com.felixlaura.pollingapp.service.PollService;
import com.felixlaura.pollingapp.util.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollService pollService;

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public UserSummary getCurrentUser(@CurrentUser UserPrincipal currentUser){
        UserSummary userSummary = new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getName());

        return userSummary;
    }

    public UserIdentityAvailability checkEmailAvailability(@RequestParam(value = "email") String email){
        Boolean isAvailable = !userRepository.existsByEmail(email);

        return new UserIdentityAvailability(isAvailable);
    }

    @GetMapping("/users/{username}")
    public UserProfile getUserProfile(@PathVariable(value = "username") String username){

        User user =userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User", "username", username));

    long pollCount = pollRepository.countByCreateBy(user.getId());
    long voteCount = voteRepository.countByUserId(user.getId());

    UserProfile userProfile = new UserProfile(user.getId(), user.getUsername(),
            user.getName(), user.getCreatedAt(), pollCount, voteCount);

    return userProfile;
    }

    @GetMapping("/users/{username}/polls")
    public PagedResponse<PollResponse> getPollsCreatedBy(@PathVariable(value = "username") String username,
                                                         @CurrentUser UserPrincipal currentUser,
                                                         @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                         @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return pollService.getPollsCreatedBy(username, currentUser, page, size);
    }


    @GetMapping("/users/{username}/votes")
    public PagedResponse<PollResponse> getPollsVotedBy(@PathVariable(value = "username") String username,
                                                       @CurrentUser UserPrincipal currentUser,
                                                       @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                       @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return pollService.getPollsVotedBy(username, currentUser, page, size);
    }


}
