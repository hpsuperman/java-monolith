package com.hpsuperman.monolith.modules.demo.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String content;

    private LocalDateTime sentAt;
}
