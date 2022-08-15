package com.felixlaura.pollingapp.service;

import com.felixlaura.pollingapp.exception.BadRequestException;
import com.felixlaura.pollingapp.exception.ResourceNotFoundException;
import com.felixlaura.pollingapp.model.*;
import com.felixlaura.pollingapp.payload.PagedResponse;
import com.felixlaura.pollingapp.payload.PollRequest;
import com.felixlaura.pollingapp.payload.PollResponse;
import com.felixlaura.pollingapp.payload.VoteRequest;
import com.felixlaura.pollingapp.repository.PollRepository;
import com.felixlaura.pollingapp.repository.UserRepository;
import com.felixlaura.pollingapp.repository.VoteRepository;
import com.felixlaura.pollingapp.security.UserPrincipal;
import com.felixlaura.pollingapp.util.AppConstants;
import com.felixlaura.pollingapp.util.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;


/**
 * PollController and UserController use PollService class to get list of polls
 * formatted in PollResponse payloads that is returned to the clients.
 */
@Service
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(PollService.class);

    public PagedResponse<PollResponse> getAllPolls(UserPrincipal currentUser, int page, int size){
        validatePageNumberAndSize(page, size);

        //Retrieve polls
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");
        Page<Poll> polls = pollRepository.findAll(pageable);

        if(polls.getTotalElements() == 0){
            return new PagedResponse<>(Collections.emptyList(),polls.getNumber(),
                    polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        //Map Polls to PollResponses containing vote count and poll creator details
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls.getContent());

        List<PollResponse> pollResponses = polls.map(poll->{
           return ModelMapper.mapPollToPollResponse(poll,
                   choiceVoteCountMap,
                   creatorMap.get(poll.getCreateBy()),
                   pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null
                   ));
        }).getContent();


        return new PagedResponse<>(pollResponses, polls.getNumber(), polls.getSize(),
                polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal currentUser, int page, int size){
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User", "username", username));

        //Retrieve all polls created by specific username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");
        Page<Poll> polls = pollRepository.findByCreateBy(user.getId(), pageable);

        //if number of polls by user is equal to 0
        if(polls.getTotalElements() == 0){
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(),
                    polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        //If polls is not equal to 0, we map Polls to PollResponse
        //containing vote counts and use details
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long,Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser,pollIds);

        List<PollResponse> pollResponses = polls.map(poll ->{
            return ModelMapper.mapPollToPollResponse(poll,
                    choiceVoteCountMap,
                    user,
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();

        return new PagedResponse<>(pollResponses, polls.getNumber(),polls.getSize(),
                polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    //Get Polls voted by user
    public PagedResponse<PollResponse> getPollsVotedBy(String username, UserPrincipal currentUser, int page, int size){
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User", "username", username));

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");
        Page<Long> userVotedPollIds = voteRepository.findVotedPollIdsByUserId(user.getId(), pageable);

        if(userVotedPollIds.getTotalElements() == 0){
            return new PagedResponse<>(Collections.emptyList(),
                    userVotedPollIds.getNumber(),
                    userVotedPollIds.getSize(),
                    userVotedPollIds.getTotalElements(),
                    userVotedPollIds.getTotalPages(),
                    userVotedPollIds.isLast());
        }

        //Retrieve all poll details from the voted pollIds
        List<Long> pollIds = userVotedPollIds.getContent();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Poll> polls = pollRepository.findByIdIn(pollIds, sort);

        //Map polls to PollResponse containing vote counts and poll creator details
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls);

        List<PollResponse> pollResponses = polls.stream().map(poll->{
                    return ModelMapper.mapPollToPollResponse(poll,
                            choiceVoteCountMap,
                            creatorMap.get(poll.getCreateBy()),
                            pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).collect(Collectors.toList());

        return new PagedResponse<>(pollResponses,
                userVotedPollIds.getNumber(),
                userVotedPollIds.getSize(),
                userVotedPollIds.getTotalElements(),
                userVotedPollIds.getTotalPages(),
                userVotedPollIds.isLast());
    }

    public Poll createPoll(PollRequest pollRequest){
        Poll poll = new Poll();
        poll.setQuestion(pollRequest.getQuestion());

        poll.getChoices().forEach(choiceRequest ->{
            poll.addChoice(new Choice(choiceRequest.getText()));
        });

        Instant now = Instant.now();
        Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                .plus(Duration.ofHours((pollRequest.getPollLength().getHours())));

        poll.setExpirationDateTime(expirationDateTime);

        return pollRepository.save(poll);
    }

    public PollResponse getPollById(Long pollId, UserPrincipal currentUser){
        Poll poll = pollRepository.findById(pollId).orElseThrow(
                ()->new ResourceNotFoundException("Poll", "id",pollId)
        );

        //Retrieve Vote Count of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        //Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreateBy())
                .orElseThrow(()-> new ResourceNotFoundException("User", "id", poll.getCreateBy()));

        //Retrieve vote done by logged in user
        Vote userVote = null;
        if(currentUser != null){
            userVote = voteRepository.findByUserIdAndPollId(currentUser.getId(), pollId);
        }

        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap,
                creator, userVote != null ? userVote.getChoice().getId(): null);
    }

    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(()-> new ResourceNotFoundException("Poll", "id", pollId));

        if(poll.getExpirationDateTime().isBefore(Instant.now())){
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.getOne(currentUser.getId());

        Choice selectedChoice = poll.getChoices()
                .stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(()->new ResourceNotFoundException("Choice", "id", voteRequest.getChoiceId()));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try{
            vote = voteRepository.save(vote);
        }catch (DataIntegrityViolationException ex){
            logger.info("User {} has already voted in Poll {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll");
        }

        //Retrieve Vote counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        //Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreateBy())
                .orElseThrow(()->new ResourceNotFoundException("User", "id", poll.getCreateBy()));

        return ModelMapper.mapPollToPollResponse(poll,
                choiceVotesMap,
                creator,
                vote.getChoice().getId());

    }

    private Map<Long, User> getPollCreatorMap(List<Poll> polls) {
        // Get Poll Creator details of the given list of polls
        List<Long> creatorIds = polls.stream()
                .map(Poll::getCreateBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return creatorMap;
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal currentUser, List<Long> pollIds) {
        // Retrieve Votes done by the logged in user to the given pollIds
        Map<Long, Long> pollUserVoteMap = null;
        if(currentUser != null){
            List<Vote> userVotes = voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds);

            pollUserVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        }
        return pollUserVoteMap;
    }

    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        // Retrieve Vote Counts of every Choice belonging to the given pollIds
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdInGroupByChoiceId(pollIds);

        Map<Long, Long> choiceVoteMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        return choiceVoteMap;
    }


    private void validatePageNumberAndSize(int page, int size) {
        if(page<0){
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if(size > AppConstants.MAX_PAGE_SIZE){
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }
}
