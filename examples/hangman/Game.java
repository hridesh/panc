import java.io.*;
import java.util.*;

class Bool{
	boolean state;
	public Bool(boolean b){
		state = b;
	}
	public boolean val(){return state;}
}

class Inty{
	int state;
	public Inty(int i){
		state = i;
	}
	public int val(){return state;}
}

class Stringy{
	String state;
	public Stringy(String s){
		state = s;
	}
	public String val(){return state;}
}

class GameState{
	public GameState(){
		password = "";
		guessesRemaining = 0;
		inProgress = false;
		victoryFlag = false;
		guessedLetters = new HashSet<Character>();
		revealed = new ArrayList<Character>();
	}
	public GameState(GameState other){
		this.password = other.password;
		this.guessesRemaining = other.guessesRemaining;
		this.inProgress = other.inProgress;
		this.victoryFlag = other.victoryFlag;
		this.guessedLetters = new HashSet<Character>(other.guessedLetters);
		this.revealed = new ArrayList<Character>(other.revealed);
	}
	public GameState copy(){
		return new GameState(this);
	}
	boolean victoryFlag;
	boolean inProgress;
	String password;
	int guessesRemaining;
	HashSet<Character> guessedLetters;
	List<Character> revealed;
}

capsule Timer(){
	boolean go = true;
	int count = 0;
	void run(){
		while(go){
			yield(1000);
			if(count > 0){
				count--;
			}
		}
	}
	Inty timeLeft(){
		return new Inty(count);
	}
	Bool setTime(int t){
		count = t;
		return new Bool(true);
	}
	void kill(){
		go = false;
	}
}

capsule Game(Timer timer){
	boolean go = true;
	void kill(){
		go = false;
		timer.kill();
	}
	GameState state = new GameState();
	private void win(){
		state.victoryFlag = true;
		state.inProgress = false;
	}
	private void lose(){
		state.victoryFlag = false;
		state.inProgress = false;
	}
	void run(){
		while(go){
			if(state.inProgress){
				if(!state.revealed.contains('_') && state.revealed.get(state.revealed.size()-1) != '_'){ //this is weird
					win();
				}
				else if(state.guessesRemaining < 1){
					lose();
				}
				else if(timer.timeLeft().val() == 0){
					lose();
				}
			}
		}
	}
	Bool active(){
		return new Bool(state.inProgress);
	}
	void startNewGame(String word, int timeLimit, int guessLimit){
		state.victoryFlag = false;
		state.password = word;
		state.guessesRemaining = guessLimit;
		state.guessedLetters.clear();
		state.revealed.clear();
		for(int i = 0; i < word.length(); i++){
			state.revealed.add('_');
		}
		boolean timeSet = timer.setTime(timeLimit).val();
		if(timeSet)
			state.inProgress = true;
	}
	void guessLetter(Character c){
		if(active().val()){
			if(state.guessedLetters.contains(c)){
				state.guessesRemaining--;
			}
			else{
				state.guessedLetters.add(c);
				if(state.password.contains(c+"")){
					int index = state.password.indexOf(c, 0);
					while(index != -1){
						state.revealed.remove(index);
						state.revealed.add(index, c);
						index = state.password.indexOf(c,index+1);
					}
				}
				else{
					state.guessesRemaining--;
				}
			}
		}
	}
	GameState copyState(){
		return state.copy();
	}
	Inty timeLeft(){
		return timer.timeLeft();
	}
}

signature View{
	void display(GameState state, int timeLeft);
	void message(String m);
}

signature InputDevice{
	Stringy readInput();
	void kill();
}