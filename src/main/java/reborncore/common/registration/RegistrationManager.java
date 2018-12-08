/*
 * Copyright (c) 2018 modmuss50 and Gigabit101
 *
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package reborncore.common.registration;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLStateEvent;
import net.minecraftforge.fml.relauncher.Side;
import reborncore.RebornCore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Mark on 26/02/2017.
 */
public class RegistrationManager {

	static List<IRegistryFactory> factoryList = new ArrayList<>();
	static List<Class> registryClasses = new ArrayList<>();

	public static void init(FMLPreInitializationEvent event) {
		long start = System.currentTimeMillis();
		ASMDataTable asmDataTable = event.getAsmData();
		loadFactorys(asmDataTable);

		Set<ASMDataTable.ASMData> asmDataSet = asmDataTable.getAll(RebornRegistry.class.getName());
		List<ASMDataTable.ASMData> asmDataList = new ArrayList<>(asmDataSet);
		asmDataList.sort(Comparator.comparingInt(RegistrationManager::getPriority));
		for (ASMDataTable.ASMData data : asmDataList) {
			try {
				if(!isModPresent(data, asmDataTable)){
					continue;
				}
				Class clazz = Class.forName(data.getClassName());
				if(isEarlyReg(data)){
					handleClass(clazz, null, factoryList);
					continue;
				}
				registryClasses.add(clazz);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Failed to tick class", e);
			}
		}

		//Sorts all the classes to (try) and ensure they are loaded in the same oder on the client/server.
		//Hopefully this fixes the issue with packets being misaligned
		registryClasses.sort(Comparator.comparing(Class::getCanonicalName));
		RebornCore.logHelper.info("Pre loaded registries in" + (System.currentTimeMillis() - start) + "ms");
	}

	private static int getPriority(ASMDataTable.ASMData asmData){
		if(asmData.getAnnotationInfo().containsKey("priority")){
			return -(int) asmData.getAnnotationInfo().get("priority");
		}
		return 0;
	}

	private static boolean isEarlyReg(ASMDataTable.ASMData asmData){
		if(asmData.getAnnotationInfo().containsKey("earlyReg")){
			return (boolean) asmData.getAnnotationInfo().get("earlyReg");
		}
		return false;
	}

	private static boolean isModPresent(ASMDataTable.ASMData asmData, ASMDataTable dataTable){
		if(!asmData.getAnnotationInfo().containsKey("modOnly")){ //Doesnt have any details about if its only to be loaded along with a specfic mod, so we assume true
			return true;
		}
		String modOnly = (String) asmData.getAnnotationInfo().get("modOnly");
		if(modOnly == null || modOnly.isEmpty()){
			return true;
		}
		for (String modid : modOnly.split(",")) {
			if (modid.startsWith("@")) {
				if (modid.equals("@client")) {
					if (FMLCommonHandler.instance().getSide() != Side.CLIENT) {
						return false;
					}
				}
			} else if (modid.startsWith("!")) {
				if (Loader.isModLoaded(modid.replaceAll("!", ""))) {
					return false;
				}
			} else {
				if (!Loader.isModLoaded(modid)) {
					return false;
				}
			}
		}
		return true;
	}

	public static void load(FMLStateEvent event) {
		long start = System.currentTimeMillis();
		final ModContainer activeMod = Loader.instance().activeModContainer();

		List<IRegistryFactory> factoryList = getFactorysForSate(event.getClass());
		if (!factoryList.isEmpty()) {
			for (Class clazz : registryClasses) {
				handleClass(clazz, activeMod, factoryList);
			}
			factoryList.forEach(IRegistryFactory::factoryComplete);
			setActiveModContainer(activeMod);
		}

		RebornCore.logHelper.info("Loaded registrys for " + event.getClass().getName() + " in " + (System.currentTimeMillis() - start) + "ms");
	}

	private static void handleClass(Class clazz, ModContainer activeMod, List<IRegistryFactory> factories){
		RebornRegistry annotation = (RebornRegistry) getAnnoation(clazz.getAnnotations(), RebornRegistry.class);
		if (annotation != null) {
			if (activeMod != null && !activeMod.getModId().equals(annotation.modID())) {
				setActiveMod(annotation.modID());
			}
		}
		for (IRegistryFactory regFactory : factories) {
			for (Field field : clazz.getDeclaredFields()) {
				if (!regFactory.getTargets().contains(RegistryTarget.FIELD)) {
					continue;
				}
				if (field.isAnnotationPresent(regFactory.getAnnotation())) {
					regFactory.handleField(field);
				}
			}
			for (Method method : clazz.getDeclaredMethods()) {
				if (!regFactory.getTargets().contains(RegistryTarget.MEHTOD)) {
					continue;
				}
				if (method.isAnnotationPresent(regFactory.getAnnotation())) {
					regFactory.handleMethod(method);
				}

			}
			if(regFactory.getTargets().contains(RegistryTarget.CLASS)){
				if(clazz.isAnnotationPresent(regFactory.getAnnotation())){
					regFactory.handleClass(clazz);
				}
			}
		}
	}

	private static List<IRegistryFactory> getFactorysForSate(Class<? extends FMLStateEvent> event) {
		return factoryList.stream().filter(iRegistryFactory -> iRegistryFactory.getProcessSate() == event).collect(Collectors.toList());
	}

	public static Annotation getAnnoationFromArray(Annotation[] annotations, IRegistryFactory factory) {
		for (Annotation annotation : annotations) {
			if (annotation.annotationType() == factory.getAnnotation()) {
				return annotation;
			}
		}
		return null;
	}

	public static Annotation getAnnoation(Annotation[] annotations, Class annoation) {
		for (Annotation annotation : annotations) {
			if (annotation.annotationType() == annoation) {
				return annotation;
			}
		}
		return null;
	}

	private static void loadFactorys(ASMDataTable dataTable) {
		Set<ASMDataTable.ASMData> asmDataSet = dataTable.getAll(IRegistryFactory.RegistryFactory.class.getName());
		for (ASMDataTable.ASMData data : asmDataSet) {
			try {
				Class clazz = Class.forName(data.getClassName());
				IRegistryFactory.RegistryFactory registryFactory = (IRegistryFactory.RegistryFactory) getAnnoation(clazz.getAnnotations(), IRegistryFactory.RegistryFactory.class);
				if (!registryFactory.side().canExcetue()) {
					continue;
				}
				if (!Loader.isModLoaded(registryFactory.modID())) {
					continue;
				}
				Object object = clazz.newInstance();
				if (object instanceof IRegistryFactory) {
					IRegistryFactory factory = (IRegistryFactory) object;
					factoryList.add(factory);
				}
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}
		}
	}

	private static void setActiveMod(String modID) {
		for (ModContainer modContainer : Loader.instance().getActiveModList()) {
			if (modContainer.getModId().equals(modID)) {
				setActiveModContainer(modContainer);
				break;
			}
		}
	}

	private static void setActiveModContainer(ModContainer container) {
		Loader.instance().setActiveModContainer(container);
	}

}
