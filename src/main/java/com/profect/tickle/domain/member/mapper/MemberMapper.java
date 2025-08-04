package com.profect.tickle.domain.member.mapper;

import com.profect.tickle.domain.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface MemberMapper {

    Optional<Member> findByEmail(String email);
}
