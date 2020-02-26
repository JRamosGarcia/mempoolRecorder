package com.mempoolrecorder.controllers.entities;

import java.util.ArrayList;
import java.util.List;

public class RangeExecutionInfo {

	private List<String> executionInfoList = new ArrayList<>();

	public List<String> getExecutionInfoList() {
		return executionInfoList;
	}

	public void setExecutionInfoList(List<String> executionInfoList) {
		this.executionInfoList = executionInfoList;
	}

}
