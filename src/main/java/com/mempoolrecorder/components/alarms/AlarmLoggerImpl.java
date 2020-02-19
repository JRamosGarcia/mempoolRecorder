package com.mempoolrecorder.components.alarms;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mempoolrecorder.utils.SysProps;

@Component
public class AlarmLoggerImpl implements AlarmLogger {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	// Concurrent optimized for high number or transversals.
	private List<String> alarmList = new CopyOnWriteArrayList<>();

	@Override
	public List<String> getAlarmList() {
		return alarmList;
	}

	@Override
	public void addAlarm(String alarm) {
		alarmList.add(alarm);
	}

	@Override
	public void prettyPrint() {
		if (!alarmList.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(SysProps.NL);
			sb.append("--------------------------------------------------------------------------");
			sb.append(SysProps.NL);
			for (String alarmStr : alarmList) {
				sb.append("- ");
				sb.append(alarmStr);
				sb.append(SysProps.NL);
			}
			sb.append("--------------------------------------------------------------------------");
			logger.warn(sb.toString());
		}
	}

}
