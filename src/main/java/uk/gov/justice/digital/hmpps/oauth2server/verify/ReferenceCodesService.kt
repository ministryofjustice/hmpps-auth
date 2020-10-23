package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.ReferenceCodeRepository
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class ReferenceCodesService(referenceCodeRepository: ReferenceCodeRepository) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val referenceCodesCache: LoadingCache<ReferenceDomain, List<String>>

  fun isValidEmailDomain(domain: String): Boolean {
    if (StringUtils.isBlank(domain)) {
      return false
    }
    val domainLower = domain.toLowerCase()
    return try {
      referenceCodesCache[ReferenceDomain.EMAIL_DOMAIN]
        .map { it.replace("%".toRegex(), ".*").toLowerCase() }
        .any { domainLower.matches(it.toRegex()) }
    } catch (e: ExecutionException) {
      // nothing we can do here, so throw toys out of pram
      log.error("Caught exception retrieving reference codes", e)
      throw RuntimeException(e)
    }
  }

  init {
    referenceCodesCache =
      CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(
        CacheLoader.from { k: ReferenceDomain? ->
          val referenceCodes =
            referenceCodeRepository.findByDomainCodeIdentifierDomainAndActiveIsTrueAndExpiredDateIsNull(k)
          referenceCodes.map { it.description }
        }
      )
  }
}
