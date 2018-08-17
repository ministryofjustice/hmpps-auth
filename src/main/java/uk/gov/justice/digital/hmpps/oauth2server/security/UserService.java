package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserCaseloadRole;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {

	private final StaffUserAccountRepository userRepository;
	private final String apiCaseloadId;

	public UserService(StaffUserAccountRepository userRepository,
					   @Value("${application.caseload.id}") String apiCaseloadId) {
		this.userRepository = userRepository;
		this.apiCaseloadId = apiCaseloadId;
	}

	public Optional<StaffUserAccount> getUserByUsername(String username) {
		return userRepository.findById(username);
	}

	public List<UserCaseloadRole> getRolesByUsername(String username, boolean allRoles) {
		StaffUserAccount user = getUserByUsername(username).orElseThrow(EntityNotFoundException::new);
		List<UserCaseloadRole> roles = user.getRoles();
		if (allRoles) return roles;

		return user.filterRolesByCaseload(apiCaseloadId);
	}

	public StaffUserAccount getUserByExternalIdentifier(String idType, String id, boolean activeOnly) {
//	    Staff staffDetail = staffService.getStaffDetailByPersonnelIdentifier(idType, id);
//
//        Optional<UserDetail> userDetail;
//
//        if (activeOnly && !StaffService.isStaffActive(staffDetail)) {
//        	log.info("Staff member found for external identifier with idType [{}] and id [{}] but not active.", idType, id);
//
//        	userDetail = Optional.empty();
//		} else {
//			userDetail = userRepository.findByStaffIdAndStaffUserType(
//					staffDetail.getStaffId(), STAFF_USER_TYPE_FOR_EXTERNAL_USER_IDENTIFICATION);
//		}
//
//		return userDetail.orElseThrow(EntityNotFoundException
//                .withMessage("User not found for external identifier with idType [{}] and id [{}].", idType, id));

		return null;
	}

	public boolean isSystemAccessDenied(String username) {
		StaffUserAccount user = getUserByUsername(username).orElseThrow(EntityNotFoundException::new);
		return user.filterByCaseload(apiCaseloadId).isEmpty();
	}
}
