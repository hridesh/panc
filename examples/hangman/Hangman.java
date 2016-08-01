import java.util.*;
import java.io.*;

capsule TextView(PrintStream out) implements View{
	void display(GameState s, int timeLeft){
		out.println("Time Remaining: "+timeLeft);
		out.println("Guesses Remaining: "+s.guessesRemaining);
		out.println();
		for(int i = 0; i < s.revealed.size(); i++){
			out.print(s.revealed.get(i)+" ");
		}
		out.println();
		out.println();
		out.print("Guessed Letters: ");
		Iterator<Character> iter = s.guessedLetters.iterator();
		while(iter.hasNext()){
			Character next = iter.next();
			out.print(next+" ");
		}
		out.println();
	}
	
	void message(String m){
		out.flush();
		out.println(m);
	}
}

capsule ScannerDevice(Scanner scan) implements InputDevice{
	Stringy readInput(){
		return new Stringy(scan.next());
	}
	
	void kill(){
		scan.close();
	}
}

capsule InputListener(InputDevice input, Game game, View view){
	boolean go = true;
	void run(){
		while(go){
			if(game.active().val()){
			String inputString = input.readInput().val();
			if(inputString.length() > 0){
				game.guessLetter(inputString.charAt(0));
				view.display(game.copyState(), game.timeLeft().val());
			}
			}
		}
	}
	void kill(){
		go = false;
		input.kill();
	}
}

capsule Refresher(View view, Game game, int rate){
	boolean go = true;
	void run(){
		while(go){
			yield(rate);
			if(game.active().val()){
				view.display(game.copyState(), game.timeLeft().val());
			}
		}
	}
	void kill(){
		go = false;
	}
}

capsule Master(){

	design {
        	InputListener listener;
        	Refresher refresher;
        	ScannerDevice in;
        	TextView view;
        	Timer timer;
        	Game game;
        	timer();
        	game(timer);
        	in(new Scanner(System.in));
        	view(System.out);
        	listener(in, game, view);
        	refresher(view, game, 5000);
	}

	void run(){
		game.startNewGame("hello", 30, 6);
		view.display(game.copyState(), game.timeLeft().val());
		while(game.active().val()){
			//wait
		}
		endgameMessage();
		
		game.startNewGame("truffle", 30, 6);
		view.display(game.copyState(), game.timeLeft().val());
		while(game.active().val()){
			//wait
		}
		endgameMessage();
		
		refresher.kill();
		listener.kill();
		game.kill();
		view.message("That's all folks!");
		
	}
	
	private void endgameMessage(){
		GameState endState = game.copyState();
		view.message("The word was \""+endState.password+"\"");
		if(endState.victoryFlag){
			view.message("YOU WIN!");
		}
		else{
			view.message("YOU LOSE!");
		}
	}
}
