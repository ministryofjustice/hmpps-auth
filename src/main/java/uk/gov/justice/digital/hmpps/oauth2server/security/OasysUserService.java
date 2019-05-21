package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.model.OasysUser;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.repository.OasysUserRepository;

import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class OasysUserService {

    private final OasysUserRepository oasysUserRepository;

    @Autowired
    public OasysUserService(final OasysUserRepository oasysUserRepository) {
        this.oasysUserRepository = oasysUserRepository;
    }

    public Optional<OasysUser> getUserByUsername(final String username) {
        return oasysUserRepository.findById(username);
    }

    public Optional<UserPersonDetails> findUser(final String username) {
        return getUserByUsername(username).map(UserPersonDetails.class::cast);
    }

}
