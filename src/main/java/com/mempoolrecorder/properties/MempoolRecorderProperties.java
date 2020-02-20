package com.mempoolrecorder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mempoolrecorder")
public class MempoolRecorderProperties {
 
}
