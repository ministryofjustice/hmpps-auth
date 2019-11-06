package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper;

import javax.persistence.criteria.*;

@Builder
@EqualsAndHashCode
public class UserFilter implements Specification<User> {
    private final String name;
    private final String roleCode;
    private final String groupCode;

    private UserFilter(final String name, final String roleCode, final String groupCode) {
        this.name = StringUtils.trimToNull(name);
        this.roleCode = StringUtils.trimToNull(roleCode);
        this.groupCode = StringUtils.trimToNull(groupCode);
    }

    @Override
    public Predicate toPredicate(@NonNull final Root<User> root, @NonNull final CriteriaQuery<?> query, @NonNull final CriteriaBuilder cb) {
        final var andBuilder = ImmutableList.<Predicate>builder();
        andBuilder.add(cb.equal(root.get("master"), true));

        if (roleCode != null) {
            andBuilder.add(cb.equal(root.join("authorities").get("roleCode"), roleCode.toUpperCase()));
        }
        if (groupCode != null) {
            andBuilder.add(cb.equal(root.join("groups").get("groupCode"), groupCode.toUpperCase()));
        }
        if (name != null) {
            andBuilder.add(buildNamePredicate(root, cb));
        }
        return cb.and(andBuilder.build().toArray(new Predicate[0]));
    }

    private Predicate buildNamePredicate(final Root<User> root, final CriteriaBuilder cb) {
        final var orBuilder = ImmutableList.<Predicate>builder();
        final var pattern = "%" + name.replace(',', ' ').replaceAll(" [ ]*", "% %") + "%";
        orBuilder.add(cb.like(root.get("email"), EmailHelper.format(pattern)));
        orBuilder.add(cb.like(root.get("username"), pattern.toUpperCase()));
        final var personJoin = root.join("person", JoinType.LEFT);
        orBuilder.add(cb.like(cb.lower(cb.concat(cb.concat(personJoin.get("firstName"), " "), personJoin.get("lastName"))), pattern.toLowerCase()));
        orBuilder.add(cb.like(cb.lower(cb.concat(cb.concat(personJoin.get("lastName"), " "), personJoin.get("firstName"))), pattern.toLowerCase()));
        return cb.or(orBuilder.build().toArray(new Predicate[0]));
    }
}
