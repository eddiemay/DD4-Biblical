package com.digitald4.biblical.util;

import com.digitald4.biblical.model.SunTimeData;
import org.joda.time.DateTime;

public interface SunTimeUtil {
  SunTimeData getSunTimeData(DateTime date);
}
