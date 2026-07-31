package zm.organization.membership;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import zm.organization.common.BaseRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MemberInviteRepository extends BaseRepository<MemberInvite> {

    Optional<MemberInvite> findByCodeHash(String codeHash);

    /**
     * Consumes one use atomically. Every precondition — not expired, not
     * revoked, uses remaining — is in the WHERE clause, so the database
     * decides the winner when two claims arrive at once: exactly one UPDATE
     * matches a row and returns 1, the other returns 0.
     *
     * <p>Doing this as read-check-write in Java would let both claims pass the
     * check before either wrote, which is precisely the race the ticket calls
     * out for {@code maxUses = 1}.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE MemberInvite i
               SET i.useCount = i.useCount + 1,
                   i.consumedAt = CASE WHEN i.useCount + 1 >= i.maxUses THEN :now ELSE i.consumedAt END
             WHERE i.id = :id
               AND i.consumedAt IS NULL
               AND i.expiresAt > :now
               AND i.useCount < i.maxUses
            """)
    int consumeOneUse(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
