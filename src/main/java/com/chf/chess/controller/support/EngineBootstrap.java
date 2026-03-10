package com.chf.chess.controller.support;

import com.chf.chess.config.Properties;
import com.chf.chess.enginee.Engine;
import com.chf.chess.enginee.EngineCallBack;
import com.chf.chess.model.EngineConfig;
import com.chf.chess.util.StringUtils;

import java.util.function.Consumer;

/**
 * 引擎协调层，负责按配置装载/切换引擎实例。
 */
public final class EngineBootstrap {

    public Engine loadEngine(String name, Properties prop, Engine current,
                             EngineCallBack callBack, Consumer<Boolean> multiPvConsumer,
                             boolean licensed) {
        if (!licensed) {
            if (current != null) {
                current.close();
            }
            return null;
        }

        try {
            if (StringUtils.isNotEmpty(name)) {
                for (EngineConfig config : prop.getEngineConfigList()) {
                    if (name.equals(config.getName())) {
                        if (current != null) {
                            current.close();
                        }
                        Engine next = new Engine(config, callBack);
                        multiPvConsumer.accept(next.getMultiPV() > 1);
                        return next;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return current;
    }
}
