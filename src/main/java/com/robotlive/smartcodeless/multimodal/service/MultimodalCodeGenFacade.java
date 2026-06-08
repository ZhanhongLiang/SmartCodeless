package com.robotlive.smartcodeless.multimodal.service;

import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import com.robotlive.smartcodeless.model.entity.User;

public interface MultimodalCodeGenFacade {

    String enrichPromptWithLayout(String message, String imageId, User loginUser, AgentStreamEmitter emitter);
}
