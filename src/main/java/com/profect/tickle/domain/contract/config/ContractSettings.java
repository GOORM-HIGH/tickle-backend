package com.profect.tickle.domain.contract.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "host.contract")
@Getter
@Setter
public class ContractSettings {

    List<BigDecimal> allowedChargeList;
}
