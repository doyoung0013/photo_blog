from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone 
from rest_framework import viewsets, status
from rest_framework.decorators import api_view
from rest_framework.response import Response
from .models import Post, Comment
from .serializers import PostSerializer, CommentSerializer
from .forms import PostForm
from django.contrib.auth.models import User
from django.core.cache import cache
from django.db.models import F

# REST API용 ViewSet
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all().order_by('-published_date')
    serializer_class = PostSerializer

    def get_serializer_context(self):
        return {'request': self.request}
    
    def create(self, request, *args, **kwargs):
        print(f"[DEBUG] 요청 데이터: {request.data}")
        print(f"[DEBUG] 요청 파일: {request.FILES}")
        
        # User 가져오기 또는 생성
        try:
            user = User.objects.first()
            if user is None:
                print("[DEBUG] User가 없어서 생성합니다.")
                user = User.objects.create_user(
                    username='default_user',
                    email='default@example.com',
                    password='defaultpassword123'
                )
            print(f"[DEBUG] 사용할 User: {user.username} (ID: {user.id})")
        except Exception as e:
            print(f"[ERROR] User 처리 중 에러: {e}")
            return Response(
                {'error': f'User 처리 오류: {str(e)}'}, 
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        
        # Serializer 생성 및 검증
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            print(f"[ERROR] Serializer 검증 실패: {serializer.errors}")
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        
        # 저장
        try:
            serializer.save(author=user, published_date=timezone.now())
            print("[DEBUG] 게시물 저장 성공!")
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        except Exception as e:
            print(f"[ERROR] 저장 중 에러: {e}")
            return Response(
                {'error': f'저장 오류: {str(e)}'}, 
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


# 웹 페이지용 함수형 View들
def post_list(request):
    posts = Post.objects.order_by('-published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})


def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    comments = post.comments.order_by('-created_at')
    return render(request, 'blog/post_detail.html', {'post': post, 'comments': comments})


def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = User.objects.first()
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_new.html', {'form': form})


def post_delete(request, pk):
    post = get_object_or_404(Post, pk=pk)
    post.delete()
    return redirect('post_list')


# 좋아요 API (로그인 없이 +1씩 증가)
@api_view(['POST'])
def like_post(request, pk):
    try:
        post = Post.objects.get(pk=pk)
        post.like_count = F('like_count') + 1
        post.save(update_fields=['like_count'])
        post.refresh_from_db()
        return Response({'like_count': post.like_count}, status=status.HTTP_200_OK)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found'}, status=status.HTTP_404_NOT_FOUND)

# 익명 댓글 작성 API
@api_view(['POST'])
def add_comment(request, pk):
    try:
        post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found'}, status=status.HTTP_404_NOT_FOUND)

    user_ip = request.META.get('REMOTE_ADDR')
    key = f"comment_limit_{user_ip}_{pk}"
    if cache.get(key):
        return Response(
            {'error': '댓글을 너무 자주 작성하고 있습니다. 잠시 후 다시 시도해주세요.'},
            status=status.HTTP_429_TOO_MANY_REQUESTS
        )
    cache.set(key, True, timeout=30)
    serializer = CommentSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save(post=post)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

# 특정 게시물의 댓글 목록 조회
@api_view(['GET'])
def get_comments(request, pk):
    try:
        post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found'}, status=status.HTTP_404_NOT_FOUND)

    comments = post.comments.order_by('-created_at')
    serializer = CommentSerializer(comments, many=True)
    return Response(serializer.data, status=status.HTTP_200_OK)