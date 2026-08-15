package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.service.Core.PersonAvatarUrlCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderboardProjectionWriter {
    private final LeaderboardRedisStore redisStore;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    public void apply(LeaderboardProjectionQueryService.ProjectionState state) {
        if (state.remove()) {
            redisStore.remove(state.scope(), state.studentCode());
            return;
        }
        avatarUrlCacheService.putFromFaceImagePath(state.personId(), state.faceImagePath());
        redisStore.upsert(state.scope(), state.studentCode(), state.score(), state.data(), state.member());
    }

    public void updateMemberIfPresent(LeaderboardProjectionQueryService.MemberState state, LeaderboardScope scope) {
        if (!state.active()) {
            redisStore.remove(scope, state.studentCode());
            return;
        }
        avatarUrlCacheService.putFromFaceImagePath(state.personId(), state.faceImagePath());
        redisStore.updateMemberIfPresent(scope, state.member());
    }
}
