package com.zarbosoft.internal.jarhacks;

import com.sun.javafx.sg.prism.NGImageView;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class ImageViewScalingAgent {
	public static void premain(String arguments, Instrumentation instrumentation) {
		new AgentBuilder.Default()
				.type(named("javafx.scene.image.ImageView"))
				.transform((builder, typeDescription, classLoader, module) -> {
					try {
						return new ByteBuddy()
								.rebase(typeDescription, ClassFileLocator.ForClassLoader.of(classLoader))
								.defineMethod("doCreatePeerPublic", NGImageView.class, Visibility.PUBLIC)
								.intercept(MethodDelegation.toConstructor(NGImageView.class))
								.method(named("doCreatePeer"))
								.intercept(MethodCall.invoke(named("doCreatePeerPublic")));
					} catch (Throwable e) {
						System.out.format("TRANSFORM FAILED %s\n", e);
						throw e;
					}
				})
				.installOn(instrumentation);
	}
}
