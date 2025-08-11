package com.profect.tickle.domain.member.mapper;

import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MemberMapper {

    Optional<Member> findByEmail(String email);

    Optional<MemberResponseDto> getHostMemberDtoByEmail(@Param(value = "email") String email);

    Optional<MemberResponseDto> getMemberDtoByEmail(@Param(value = "email") String email);
}
