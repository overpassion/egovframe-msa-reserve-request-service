package org.egovframe.cloud.reserverequestservice.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
/**
 * org.egovframe.cloud.reserverequestservice.domain.ReserveRepositoryImpl
 *
 * 예약 도메인 repository custom interface 구현체
 *
 * @author 표준프레임워크센터 shinmj
 * @version 1.0
 * @since 2021/09/27
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/09/27    shinmj       최초 생성
 * </pre>
 */
@RequiredArgsConstructor
public class ReserveRepositoryImpl implements ReserveRepositoryCustom {

    private final R2dbcEntityTemplate entityTemplate;

    /**
     * 예약 insert
     * pk(reserveId)를 서비스에서 생성하여 insert 하기 위함.
     *
     * @param reserve
     * @return
     */
    @Override
    public Mono<Reserve> insert(Reserve reserve) {
        return entityTemplate.insert(reserve);
    }
}
