package com.profect.tickle.domain.member.service;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createMember(CreateMemberRequestDto createUserRequest) {
        Member newMember = Member.createMember(createUserRequest);
        newMember.encryptPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        memberRepository.save(newMember);
    }

    // 로그인 요청 시 AuthenticationManager를 통해서 호출 될 메서드
    @Override
    public UserDetails loadUserByUsername(String inputEmail) throws UsernameNotFoundException {
        // 인증 토큰에 담긴 email이 메서드로 넘어오므로 해당 값을 기준으로 DB에서 조회한다.

        Member signInMember = memberRepository.findByEmail(inputEmail)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        grantedAuthorityList.add(new SimpleGrantedAuthority(signInMember.getMemberRole().name()));

        // 내부적으로 비밀번호가 일치하는 확인도 한다.
        return new User(signInMember.getEmail(), signInMember.getPassword(), grantedAuthorityList);
    }
}
