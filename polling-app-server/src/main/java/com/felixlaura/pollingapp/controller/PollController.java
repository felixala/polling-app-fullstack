package com.felixlaura.pollingapp.controller;

import com.felixlaura.pollingapp.model.Poll;
import com.felixlaura.pollingapp.payload.*;
import com.felixlaura.pollingapp.repository.PollRepository;
import com.felixlaura.pollingapp.repository.UserRepository;
import com.felixlaura.pollingapp.repository.VoteRepository;
import com.felixlaura.pollingapp.security.CurrentUser;
import com.felixlaura.pollingapp.security.UserPrincipal;
import com.felixlaura.pollingapp.service.PollService;
import com.felixlaura.pollingapp.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

/**
 * Create a Poll
 * Get a paginated list of polls sorted by their creation time
 * Get a Poll by pollId;
 * Vote for a choice in a poll
 */

@RestController
@RequestMapping("/api/polls")
public class PollController {

    @Autowired
    private PollService pollService;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    @GetMapping
    public PagedResponse<PollResponse> getPolls(@CurrentUser UserPrincipal userPrincipal,
                                                @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size)
            {

    return pollService.getAllPolls(userPrincipal, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPoll(@Valid @RequestBody PollRequest pollRequest){
        Poll poll = pollService.createPoll(pollRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{pollId}")
                .buildAndExpand(poll.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "Poll created successfully"));
    }

    @GetMapping("/{pollId}")
    public PollResponse getPollBodyId(@CurrentUser UserPrincipal currentUser,
                                      @PathVariable Long pollId){
        return pollService.getPollById(pollId, currentUser);
    }

    public PollResponse castVote(@CurrentUser UserPrincipal currentUser,
                                 @PathVariable Long pollId,
                                 @Valid @RequestBody VoteRequest voteRequest){
        return pollService.castVoteAndGetUpdatedPoll(pollId,voteRequest, currentUser);
    }
}
