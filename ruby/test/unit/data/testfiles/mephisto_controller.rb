class MephistoController < ApplicationController
  layout nil
  session :off
  caches_page_with_references :dispatch
  cache_sweeper :comment_sweeper

  def dispatch
    @dispatch_path    = Mephisto::Dispatcher.run(site, params[:path].dup)
    @dispatch_action  = @dispatch_path.shift
    @section          = @dispatch_path.shift
    @dispatch_action == :error ? show_404 : send("dispatch_#{@dispatch_action}")
  end

  protected
  def dispatch_redirect
    @skip_caching = true
    # @section is the http status
    # @dispatch_path.first has the headers
    if @dispatch_path.first.is_a?(Hash)
      response.headers['Status'] = interpret_status @section
      redirect_to @dispatch_path.first[:location]
    else
      head @section
    end
  end

  def dispatch_list
    @articles = @section.articles.find_by_date(:include => :user, :limit => @section.articles_per_page)
    render_liquid_template_for(@section.show_paged_articles? ? :page : :section, 
      'section'  => @section.to_liquid(true), 'articles' => @articles)
  end

  def dispatch_page
    @article = @dispatch_path.empty? ? @section.articles.find_by_position : @section.articles.find_by_permalink(@dispatch_path.first)
    show_404 and return unless @article
    Mephisto::Liquid::CommentForm.article = @article
    render_liquid_template_for(:page, 'section' => @section.to_liquid(true),
      'article' => @article.to_liquid(:mode => :single))
  end
    
  def dispatch_comments
    show_404 and return unless find_article
    if !request.post? || params[:comment].blank?
      @skip_caching = true
      redirect_to site.permalink_for(@article) and return
    end

    @comment = @article.comments.build(params[:comment].merge(:author_ip => request.remote_ip, :user_agent => request.user_agent, :referrer => request.referer))
    @comment.check_approval site, request if @comment.valid?
    @comment.save!
    redirect_to dispatch_path(:path => (site.permalink_for(@article)[1..-1].split('/') << 'comments' << @comment.id.to_s), :anchor => @comment.dom_id)
  rescue ActiveRecord::RecordInvalid
    show_article_with 'errors' => @comment.errors.full_messages, 'submitted' => params[:comment]
  rescue Article::CommentNotAllowed
    show_article_with 'errors' => ["Commenting has been disabled on this article"]
  end
    
  def dispatch_comment
    show_article_with 'message' => 'Thanks for the comment!'
  end

  def dispatch_archives
    year  = @dispatch_path.shift
    month = @dispatch_path.shift
    if year
      month ||= '1'
    else
      year  = Time.now.utc.year
      month = Time.now.utc.month
    end
    @articles = @section.articles.find_all_in_month(year, month, :include => :user)
    render_liquid_template_for(:archive, 'section' => @section, 'articles' => @articles, 'archive_date' => Time.utc(year, month))
  end

  def dispatch_search
    conditions     = ['(published_at IS NOT NULL AND published_at <= :now) AND (title LIKE :q OR excerpt LIKE :q OR body LIKE :q)', 
      { :now => Time.now.utc, :q => "%#{params[:q]}%" }]
    search_count   = site.articles.count(:all, :conditions => conditions)
    @article_pages = Paginator.new self, search_count, site.articles_per_page, params[:page]
    @articles      = site.articles.find(:all, :conditions => conditions, :order => 'published_at DESC',
      :include => [:user, :sections],
      :limit   =>  @article_pages.items_per_page,
      :offset  =>  @article_pages.current.offset)
      
    render_liquid_template_for(:search, 'articles'      => @articles,
      'previous_page' => paged_search_url_for(@article_pages.current.previous),
      'next_page'     => paged_search_url_for(@article_pages.current.next),
      'search_string' => params[:q],
      'search_count'  => search_count)
    @skip_caching = true
  end
    
  def dispatch_tags
    @articles = site.articles.find_all_by_tags(@dispatch_path, site.articles_per_page)
    render_liquid_template_for(:tag, 'articles' => @articles, 'tags' => @dispatch_path)
  end

  def dispatch_comments_feed
    show_404 and return unless find_article
    @feed_title = "Comments"
    @comments = @article.comments
    @comments.reverse!
    render :action => 'feed', :content_type => 'application/xml'
  end

  def dispatch_changes_feed
    show_404 and return unless find_article
    @feed_title = "Changes"
    @articles = @article.versions.find(:all, :include => :updater, :order => 'version desc')
    render :action => 'feed', :content_type => 'application/xml'
  end

  def paged_search_url_for(page)
    page ? site.search_url(params[:q], page) : ''
  end

  def find_article
    cached_references << (@article = site.articles.find_by_permalink(@dispatch_path.first))
    @article
  end

  def show_article_with(assigns = {})
    find_article if @article.nil?
    show_404 and return unless @article || find_article
    Mephisto::Liquid::CommentForm.article = @article
    @article = @article.to_liquid(:mode => :single)
    render_liquid_template_for(:single, assigns.merge('articles' => [@article], 'article' => @article))
  end
  alias dispatch_single show_article_with
end
