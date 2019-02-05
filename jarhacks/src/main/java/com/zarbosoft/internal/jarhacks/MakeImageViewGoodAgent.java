package com.zarbosoft.internal.jarhacks;

import com.sun.javafx.sg.prism.NGImageView;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class MakeImageViewGoodAgent {
	public static void premain(String arguments, Instrumentation instrumentation) {
		System.out.format("agent premain\n");
		new AgentBuilder.Default()
				.type(named("javafx.scene.image.ImageView"))
				.transform((builder, typeDescription, classLoader, module) -> {
					System.out.format("TRANSFORMING %s\n", typeDescription);
					System.out.format("a1");
					DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<Object> x;
					System.out.format("a2");
					try {
						System.out.format("a3");
						final DynamicType.Builder<Object> rebase = new ByteBuddy().rebase(
								typeDescription,
								ClassFileLocator.ForClassLoader.of(classLoader)
						);
						System.out.format("a3.1");
						final DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial<Object>
								doCreatePeerPublic =
								rebase.defineMethod("doCreatePeerPublic", NGImageView.class, Visibility.PUBLIC);
						System.out.format("a3.2");
						final DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<Object> intercept =
								doCreatePeerPublic.intercept(MethodDelegation.toConstructor(NGImageView.class));
						System.out.format("a3.3");
						final DynamicType.Builder.MethodDefinition.ImplementationDefinition<Object> doCreatePeer =
								intercept.method(named("doCreatePeer"));
						System.out.format("a3.4");
						x = doCreatePeer
								.intercept(MethodCall.invoke(named("doCreatePeerPublic")));
						System.out.format("a4");
					} catch (Throwable e) {
						System.out.format("TRANSFORM FAILED %s\n", e);
						throw e;
					}
					System.out.format("a5");
					try {
						System.out.format("out %s\n", x.make().saveIn(new File("/home/andrew/bytebuddy_")));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return x;
				})
				.installOn(instrumentation);
	}
}
