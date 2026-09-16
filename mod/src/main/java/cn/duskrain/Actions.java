package cn.duskrain;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Finite protocol vocabulary; the client cannot supply a command to execute. */
public final class Actions {
    public enum Kind { OPEN,CHOOSE,CAST,BUY,SELL,PRODUCT,MAIN_REWARD,DAILY_REWARD,SIGN,MEDITATE,BREAKTHROUGH,WARP,CLAIM_ADD,CLAIM_REMOVE,HOME_SET,HOME,PARTY_INVITE,PARTY_ACCEPT,PARTY_LEAVE,TRIAL_ENTER,TRIAL_LEAVE,ARENA,SIDEBAR_TOGGLE,SIDEBAR_SCALE,ADMIN,BUILD,CITY_JUMP,CLAIM_TRUST,CLAIM_UNTRUST,CLAIM_VIEW,STALL_OPEN,STALL_BUY,STALL_LIST,STALL_PURCHASE,STALL_TAKE,SHOP_PAGE,GUILD,FLIGHT,TITLE }
    static final Map<UUID,Long> SEEN=new HashMap<>();
    static long counter=System.currentTimeMillis();
    static Network.Action parse(String command){String[] a=command.replaceFirst("^/?dr ?","").split(" ");String key=a[0],arg=a.length>1?a[1]:"",target=a.length>2?a[2]:"";int value=0;Kind k;
        switch(key){
            case "cultivate"->{k=switch(arg){case "choose"->Kind.CHOOSE;case "sit"->Kind.MEDITATE;case "breakthrough"->Kind.BREAKTHROUGH;default->Kind.OPEN;};if(k==Kind.CHOOSE)value=Integer.parseInt(target);else target="profile";}
            case "skills"->{k=arg.equals("cast")?Kind.CAST:Kind.OPEN;if(k==Kind.CAST)value=Integer.parseInt(target);else target="skills";}
            case "shop"->{k=switch(arg){case "buy"->Kind.BUY;case "sell"->Kind.SELL;case "inspect"->Kind.PRODUCT;case "page"->Kind.SHOP_PAGE;default->Kind.OPEN;};if(k==Kind.OPEN)target="shop:"+(target.isBlank()?"material":target);if(k==Kind.BUY||k==Kind.SELL||k==Kind.SHOP_PAGE)value=a.length>3?Integer.parseInt(a[3]):k==Kind.SHOP_PAGE?0:1;}
            case "quests"->{k=arg.equals("claim")?Kind.MAIN_REWARD:Kind.OPEN;if(k==Kind.MAIN_REWARD)value=Integer.parseInt(target);else target="quests";}
            case "daily"->{k=arg.equals("claim")?Kind.DAILY_REWARD:arg.equals("sign")?Kind.SIGN:Kind.OPEN;if(k==Kind.DAILY_REWARD)value=Integer.parseInt(target);else target="daily";}
            case "warp","spawn"->{k=arg.isBlank()&&!key.equals("spawn")?Kind.OPEN:Kind.WARP;target=key.equals("spawn")?"spawn":arg.isBlank()?"warp":arg;}
            case "claim"->{k=switch(arg){case "add"->Kind.CLAIM_ADD;case "remove"->Kind.CLAIM_REMOVE;case "trust"->Kind.CLAIM_TRUST;case "untrust"->Kind.CLAIM_UNTRUST;case "members","list","border"->Kind.CLAIM_VIEW;default->Kind.OPEN;};if(k==Kind.OPEN)target="claim";else if(k==Kind.CLAIM_VIEW)target=arg+(arg.equals("border")&&!target.isEmpty()?" "+target:"");}
            case "guild"->{k=arg.isEmpty()?Kind.OPEN:Kind.GUILD;target=arg.isEmpty()?"guild":arg+(target.isEmpty()?"":" "+target);}
            case "flight"->{k=Kind.FLIGHT;target=arg;if(arg.equals("rent"))value=Integer.parseInt(a[2]);}
            case "title"->{k=Kind.TITLE;target=arg;}
            case "city_jump"->k=Kind.CITY_JUMP;
            case "stalls"->{k=switch(arg){case "buy"->Kind.STALL_BUY;case "sell"->Kind.STALL_LIST;case "purchase"->Kind.STALL_PURCHASE;case "take"->Kind.STALL_TAKE;default->Kind.STALL_OPEN;};if(k==Kind.STALL_BUY)value=Integer.parseInt(target);if(k==Kind.STALL_LIST){value=Integer.parseInt(target);target=a.length>3?a[3]:"1";}}
            case "home"->{k=arg.equals("set")?Kind.HOME_SET:Kind.HOME;}
            case "party"->{k=switch(arg){case "invite"->Kind.PARTY_INVITE;case "accept"->Kind.PARTY_ACCEPT;case "leave"->Kind.PARTY_LEAVE;default->Kind.OPEN;};if(k==Kind.OPEN)target="party";}
            case "trial"->{k=arg.equals("enter")?Kind.TRIAL_ENTER:arg.equals("leave")?Kind.TRIAL_LEAVE:Kind.OPEN;if(k==Kind.TRIAL_ENTER)value=Integer.parseInt(target);else target="trial";}
            case "arena"->k=Kind.ARENA;
            case "sidebar"->{k=arg.equals("toggle")?Kind.SIDEBAR_TOGGLE:arg.equals("scale")?Kind.SIDEBAR_SCALE:Kind.OPEN;if(k==Kind.SIDEBAR_SCALE)value=Math.round(Float.parseFloat(target)*100);else target="sidebar";}
            case "admin","build"->{k=key.equals("admin")?Kind.ADMIN:Kind.BUILD;target=String.join(" ",Arrays.copyOfRange(a,1,a.length));}
            default->{k=Kind.OPEN;target=key;}
        }return new Network.Action(k,target,value,++counter);
    }
    static String command(Network.Action a,ServerPlayer p){String t=a.target();int v=a.value();if(t.length()>128||t.contains("\n")||t.contains("\r"))return null;
        boolean id=t.matches("[a-z0-9_]+"),player=t.matches("[A-Za-z0-9_]{1,16}");
        return switch(a.kind()){
            case OPEN->Set.of("menu","profile","skills","quests","daily","warp","claim","party","trial","sidebar","guide","start","guild","enchant").contains(t)?"dr "+t:Set.of("shop:material","shop:alchemy","shop:forge","shop:elixir","shop:treasure").contains(t)?"dr shop view "+t.substring(5):null;
            case GUILD->Set.of("accept","enter","return","leave","members","disband","confirm").contains(t)||t.matches("(?:create|invite|kick|officer|transfer) [\\p{L}\\p{N}_-]{1,36}")?"dr guild "+t:null;
            case SHOP_PAGE->t.matches("(?:forge|alchemy|material|elixir|treasure):(?:all|artifact|robe):(?:-1|[0-6]):(?:all|helmet|chestplate|leggings|boots)")&&v>=0&&v<=100000?"dr shop page \""+t+"\" "+v:null;
            case CHOOSE->v>=1&&v<=3?"dr cultivate choose "+v:null;
            case CAST->v>=0&&v<=2?"dr skills cast "+v:null;
            case BUY,SELL->id&&v>=1&&v<=64?"dr shop "+(a.kind()==Kind.BUY?"buy":"sell")+" "+t+" "+v:null;
            case PRODUCT->id?"dr shop inspect "+t:null;
            case MAIN_REWARD->v>=0&&v<24?"dr quests claim "+v:null;
            case DAILY_REWARD->v>=0&&v<12?"dr daily claim "+v:null;
            case SIGN->"dr daily sign";case MEDITATE->"dr cultivate sit";case BREAKTHROUGH->"dr cultivate breakthrough";
            case WARP->id?(t.equals("spawn")?"dr spawn":"dr warp "+t):null;
            case CLAIM_ADD->"dr claim add";case CLAIM_REMOVE->"dr claim remove";case HOME_SET->"dr home set";case HOME->"dr home";
            case PARTY_INVITE->player?"dr party invite "+t:null;case PARTY_ACCEPT->"dr party accept";case PARTY_LEAVE->"dr party leave";
            case TRIAL_ENTER->v>=1&&v<=6?"dr trial enter "+v:null;case TRIAL_LEAVE->"dr trial leave";
            case ARENA->"dr arena";case SIDEBAR_TOGGLE->"dr sidebar toggle";case SIDEBAR_SCALE->v>=65&&v<=150?"dr sidebar scale "+(v/100f):null;
            case FLIGHT->Set.of("","toggle").contains(t)?("dr flight "+t).strip():t.equals("rent")&&v>=0&&v<=1?"dr flight rent "+v:null;
            case TITLE->Set.of("","toggle").contains(t)?("dr title "+t).strip():null;
            case CITY_JUMP->"dr city_jump";
            case CLAIM_TRUST,CLAIM_UNTRUST->player||t.matches("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}")?"dr claim "+(a.kind()==Kind.CLAIM_TRUST?"trust":"untrust")+" "+t:null;
            case CLAIM_VIEW->Set.of("members","list","border","border auto","border on","border off").contains(t)?"dr claim "+t:null;
            case STALL_OPEN->"dr stalls";
            case STALL_BUY->v>=0&&v<PlayerMarket.SITES.size()?"dr stalls buy "+v:null;
            case STALL_LIST->v>=1&&v<=100000000&&t.matches("[1-9]|[1-5][0-9]|6[0-4]")?"dr stalls sell "+v+" "+t:null;
            case STALL_PURCHASE,STALL_TAKE->t.matches("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}")?"dr stalls "+(a.kind()==Kind.STALL_TAKE?"take ":"purchase ")+t:null;
            case ADMIN->p.hasPermissions(3)&&Set.of("","maintenance","reload","pregen","audit").contains(t)?("dr admin "+t).strip():null;
            case BUILD->p.hasPermissions(3)&&(Set.of("","pause","resume","start city","start palace","camera palace","camera aerial","camera orbit","camera off","record true","record false").contains(t)||t.matches("speed (100|600|4000)"))?("dr build "+t).strip():null;
        };
    }
    static void receive(ServerPlayer p,Network.Action a){if(a.kind()==Kind.GUILD&&Guilds.receive(p,a))return;String c=command(a,p);if(c==null||a.sequence()<=SEEN.getOrDefault(p.getUUID(),-1L))return;SEEN.put(p.getUUID(),a.sequence());long now=System.nanoTime(),last=Network.LAST_ACTION.getOrDefault(p.getUUID(),0L);if(now-last<80_000_000L)return;Network.LAST_ACTION.put(p.getUUID(),now);p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),c);}
}
