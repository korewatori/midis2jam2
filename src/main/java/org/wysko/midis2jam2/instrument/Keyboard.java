package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Keyboard extends Instrument {
	
	public static final int A_0 = 21;
	static final int KEYBOARD_KEY_COUNT = 88;
	private final List<? extends MidiEvent> events;
	private final MidiFile file;
	private final Skin skin;
	public Node movementNode = new Node();
	public Node node = new Node();
	public KeyboardKey[] keys = new KeyboardKey[KEYBOARD_KEY_COUNT];
	
	public Keyboard(Midis2jam2 context, List<? extends MidiEvent> events, MidiFile file, Skin skin) {
		this.context = context;
		this.events = events;
		this.file = file;
		this.skin = skin;
		Spatial pianoCase = context.loadModel("PianoCase.obj", skin.textureFile);
		
		int whiteCount = 0;
		for (int i = 0; i < KEYBOARD_KEY_COUNT; i++) {
			if (midiValueToColor(i + A_0) == KeyColor.WHITE) { // White key
				keys[i] = new KeyboardKey(context, i + A_0, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new KeyboardKey(context, i + A_0, i);
			}
		}
		
		movementNode.attachChild(pianoCase);
		node.attachChild(movementNode);
		context.getRootNode().attachChild(node);
		
		node.move(-50, 32f, -6);
		node.rotate(0, rad(45), 0);
	}
	
	/**
	 * Calculates if a MIDI note value is a black or white key on a standard piano.
	 *
	 * @param x the MIDI note value
	 * @return {@link KeyColor#WHITE} or {@link KeyColor#BLACK}
	 */
	static KeyColor midiValueToColor(int x) {
		x = x % 12;
		return x == 1
				|| x == 3
				|| x == 6
				|| x == 8
				|| x == 10 ? KeyColor.BLACK : KeyColor.WHITE;
	}
	
	KeyboardKey getKeyByMIDINote(int midiNote) {
		return keys[midiNote - A_0];
	}
	
	public void tick(double x, float delta) {
		// Move if overlapping
		int keyboardsBeforeMe = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (context.instruments.get(i) instanceof Keyboard &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				keyboardsBeforeMe++;
			}
		}
		movementNode.setLocalTranslation(0, keyboardsBeforeMe * 3.030f, -keyboardsBeforeMe * (5.865f));
		
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		if (!events.isEmpty()) {
			if (!(events.get(0) instanceof MidiNoteOnEvent) && !(events.get(0) instanceof MidiNoteOffEvent)) {
				events.remove(0);
			}
			while (!events.isEmpty() && ((events.get(0) instanceof MidiNoteOnEvent && file.eventInSeconds(events.get(0)) <= x) ||
					(events.get(0) instanceof MidiNoteOffEvent && file.eventInSeconds(events.get(0)) - x <= 0.05))) {
				eventsToPerform.add(events.remove(0));
			}
		}
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				pushKeyDown(((MidiNoteOnEvent) event).note);
			} else if (event instanceof MidiNoteOffEvent) {
				releaseKey(((MidiNoteOffEvent) event).note);
			}
		}
		
		transitionAnimation(delta);
	}
	
	public void pushKeyDown(int midiNote) {
		KeyboardKey key = keys[midiNote - Keyboard.A_0];
		key.node.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
		key.beingPressed = true;
	}
	
	public void transitionAnimation(float delta) {
		for (KeyboardKey key : keys) {
			if (!key.beingPressed) {
				float[] angles = new float[3];
				key.node.getLocalRotation().toAngles(angles);
				if (angles[0] > 0.0001) { // fuck floats
					key.node.setLocalRotation(new Quaternion(new float[]
							{Math.max(angles[0] - (0.02f * delta * 50), 0), 0, 0}
					));
//					key.node.rotate(-0.02f * delta * 50, 0, 0);
				} else {
					key.node.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
				}
			}
		}
	}
	
	public void releaseKey(int midiNote) {
		KeyboardKey key = keys[midiNote - Keyboard.A_0];
		key.beingPressed = false;
	}
	
	public enum Skin {
		HARPSICHORD("HarpsichordSkin.bmp"),
		PIANO("PianoSkin.bmp"),
		SYNTH("SynthSkin.bmp"),
		WOOD("PianoSkin_Wood.bmp");
		String textureFile;
		
		Skin(String textureFile) {
			this.textureFile = textureFile;
		}
	}
	
	enum KeyColor {
		WHITE, BLACK
	}
	
	public class KeyboardKey {
		public int midiNote;
		public Node node = new Node();
		public boolean beingPressed = false;
		
		public KeyboardKey(Midis2jam2 context, int midiNote, int startPos) {
			this.midiNote = midiNote;
			
			
			if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
				// Front key
				Spatial keyFront = context.loadModel("PianoWhiteKeyFront.obj", skin.textureFile);
				
				// Back key
				Spatial keyBack = context.loadModel("PianoWhiteKeyBack.obj", skin.textureFile);
				
				node.attachChild(keyFront);
				node.attachChild(keyBack);
				
				Keyboard.this.movementNode.attachChild(node);
				node.move(startPos - 26, 0, 0); // 26 = count(white keys) / 2
			} else { // Black key
				Spatial blackKey = context.loadModel("PianoBlackKey.obj", skin.textureFile);
				node.attachChild(blackKey);
				
				Keyboard.this.movementNode.attachChild(node);
				node.move(midiNote * (7 / 12f) - 38.2f, 0, 0);
			}
			
		}
	}
}
