package org.egovframe.cloud.reserverequestservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.reserverequestservice.api.dto.ReserveResponseDto;
import org.egovframe.cloud.reserverequestservice.api.dto.ReserveSaveRequestDto;
import org.egovframe.cloud.reserverequestservice.config.MessageListenerContainerFactory;
import org.egovframe.cloud.reserverequestservice.service.ReserveService;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * org.egovframe.cloud.reserverequestservice.api.ReserveApiController
 *
 * 예약 신청 rest controller class
 *
 * @author 표준프레임워크센터 shinmj
 * @version 1.0
 * @since 2021/09/16
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/09/16    shinmj       최초 생성
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class ReserveApiController {

    private final ReserveService reserveService;
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final AmqpAdmin amqpAdmin;

    /**
     * 예약 신청 - 심사
     *
     * @param saveRequestDto
     * @return
     */
    @PostMapping("/api/v1/requests/audit")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReserveResponseDto> create(@RequestBody Mono<ReserveSaveRequestDto> saveRequestDto) {
        return reserveService.create(saveRequestDto);
    }

    /**
     * 예약 신청 - 실시간
     *
     * @param saveRequestDto
     * @return
     */
    @PostMapping("/api/v1/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReserveResponseDto> save(@RequestBody Mono<ReserveSaveRequestDto> saveRequestDto) {
       return reserveService.save(saveRequestDto);
    }

    /**
     * 실시간 예약 신청 후 결과 여부 subscribe
     *
     * @param reserveId
     * @return
     */
    @GetMapping(value = "/api/v1/requests/direct/{reserveId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<?> receiveReservationResult(@PathVariable String reserveId) {
        MessageListenerContainer mlc = messageListenerContainerFactory.createMessageListenerContainer(reserveId);
        Flux<String> f = Flux.create(emitter -> {
          mlc.setupMessageListener((MessageListener) m -> {
              String qname = m.getMessageProperties().getConsumerQueue();
              log.info("message received, queue={}", qname);

              if (emitter.isCancelled()) {
                  log.info("cancelled, queue={}", qname);
                  mlc.stop();
                  return;
              }

              String payload = new String(m.getBody());
              log.info("message data = {}", payload);
              emitter.next(payload);

              log.info("message sent to client, queue={}", qname);
          });

          emitter.onRequest(v -> {
              log.info("starting container, queue={}", reserveId);
              mlc.start();
          });

          emitter.onDispose(() -> {
              log.info("on dispose, queue={}", reserveId);
              mlc.stop();
              amqpAdmin.deleteQueue(reserveId);
          });

            log.info("container started, queue={}", reserveId);
        });

        return Flux.interval(Duration.ofSeconds(5))
                .map(v -> {
                    log.info("sending keepalive message...");
                    return "no news is good news";
                }).mergeWith(f);
    }

}
