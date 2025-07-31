package com.profect.tickle.domain.member.service;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createMember(CreateMemberRequestDto createUserRequest) {
        Member newMember = Member.createMember(createUserRequest);
        newMember.encryptPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        memberRepository.save(newMember);
    }
}
